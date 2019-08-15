package cmpe.dos.controller;

import cmpe.dos.dto.CouponDto;
import cmpe.dos.entity.CouponDict;
import cmpe.dos.entity.Reward;
import cmpe.dos.response.JsonResponse;
import cmpe.dos.service.CouponDictService;
import cmpe.dos.service.RewardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;

import static cmpe.dos.config.security.UserRole.PRIV_ADMIN;
import static cmpe.dos.constant.UrlConstant.COUPON;
import static cmpe.dos.constant.UrlConstant.REWARD;


/**
 * Created by Vencci on 01/12/2017.
 */
@RestController
@Api(tags = {"Reward"})
@SwaggerDefinition(tags = { @Tag(name="Reward Controller", description="Distribute Coupons to Users")})
@Transactional(rollbackFor = Exception.class)
public class RewardController extends AbstractController{
    
    @Autowired
    RewardService rewardSvc;
    
    @Autowired
    CouponDictService couponSvc;

    @ApiOperation(value = "Distribute Coupon to Loyal Customer", response = JsonResponse.class)
    @PostMapping(REWARD)
    @PreAuthorize(PRIV_ADMIN)
    public ResponseEntity<JsonResponse> giveCoupon(@RequestBody CouponDto couponDto) {
        Boolean isGiven = rewardSvc.addNewCoupon(couponDto);
        return success("RewardGiven", isGiven);
    }
    
    @ApiOperation(value = "List My Rewards", response = JsonResponse.class)
    @GetMapping(REWARD + "/self")
    public ResponseEntity<JsonResponse> getMyRewards(Principal principal){
	List<Reward> rewards = rewardSvc.getRewardsByUser(principal.getName());
	if (rewards != null && !rewards.isEmpty())
	    return success("rewards", rewards);
	
	return notFound();
    }
    
    
    @ApiOperation(value = "Get Rewards By User", response = JsonResponse.class)
    @GetMapping(REWARD + "/{username}")
    @PreAuthorize(PRIV_ADMIN)
    public ResponseEntity<JsonResponse> getRewardByUser(@PathVariable String username){
	List<Reward> rewards = rewardSvc.getRewardsByUser(username);
	if (rewards != null && !rewards.isEmpty())
	    return success("rewards", rewards);
	
	return notFound();
    }
    
    @ApiOperation(value = "List All Coupons", response = JsonResponse.class)
    @GetMapping(COUPON)
    @PreAuthorize(PRIV_ADMIN)
    public ResponseEntity<JsonResponse> getCoupons(){
	List<CouponDict> coupons = couponSvc.getCoupons();
	if (coupons != null && !coupons.isEmpty())
	    return success("coupons", coupons);
	
	return notFound();
    }
    
    @ApiOperation(value = "Find a Coupon", response = JsonResponse.class)
    @GetMapping(COUPON + "/{couponId}")
    public ResponseEntity<JsonResponse> getCouponByName(@PathVariable String couponId){
	CouponDict coupon = couponSvc.getCouponInfo(couponId);
	
	if (coupon != null)
	    return success("coupon", coupon);
	
	return notFound();
    }
    

    
    
}
