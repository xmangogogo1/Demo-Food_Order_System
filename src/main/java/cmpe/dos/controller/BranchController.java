package cmpe.dos.controller;

import cmpe.dos.entity.Branch;
import cmpe.dos.response.JsonResponse;
import cmpe.dos.service.BranchService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import static cmpe.dos.constant.UrlConstant.BRANCH;

@CrossOrigin
@RestController
@Api(tags = {"Branch"})
@SwaggerDefinition(tags = { @Tag(name="Branch Controller", description="Branch Controller Endpoints")})
@Transactional(rollbackFor = Exception.class)
public class BranchController extends AbstractController {

    @Autowired
    BranchService branchSvc;
    
    @ApiOperation(value = "Get All Branches")
    @GetMapping(BRANCH)
    public ResponseEntity<JsonResponse> getMyUserName(){
	List<Branch> branchList = branchSvc.listBranches();
	
	if (!branchList.isEmpty())
	    return success("branches", branchList);
	
	return notFound();
    }

    @ApiOperation(value = "View Dishes By Branch")
    @GetMapping("Branch/Catalog/Dish/{branchId}")
    public ResponseEntity<JsonResponse> getDishesByBranch(@PathVariable Short branchId){
        return success("Results", branchSvc.getBranchCatalogDish(branchId));
    }
}
