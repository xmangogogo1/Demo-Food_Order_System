package cmpe.dos.controller;

import static org.slf4j.LoggerFactory.getLogger;

import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import cmpe.dos.dto.OrderHistoryDto;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;

import cmpe.dos.service.*;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import cmpe.dos.config.security.AuthConfig;
import cmpe.dos.dto.CreditInfoDto;
import cmpe.dos.dto.DeliverInfoDto;
import cmpe.dos.dto.OrderDetailDto;
import cmpe.dos.entity.DeliveryInfo;
import cmpe.dos.entity.Dish;
import cmpe.dos.entity.DishDict;
import cmpe.dos.entity.Order;
import cmpe.dos.entity.OrderDishDetail;
import cmpe.dos.entity.OrderPayInfo;
import cmpe.dos.entity.Reward;
import cmpe.dos.exception.AppException;
import cmpe.dos.mapper.CreditInfoMapper;
import cmpe.dos.mapper.DeliverInfoMapper;
import cmpe.dos.mapper.DishMapper;
import cmpe.dos.response.JsonResponse;
import cmpe.dos.utility.Param;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.SwaggerDefinition;
import io.swagger.annotations.Tag;

@CrossOrigin
@RestController
@Api(tags = { "Order" })
@SwaggerDefinition(tags = { @Tag(name = "Order Controller", description = "Create an order") })

@Transactional(rollbackFor = Exception.class)
public class OrderController extends AbstractController {

    private final static Logger LOGGER = getLogger(OrderController.class);

    @Autowired
    OrderService orderService;

    @Autowired
    DeliverySettingService deliverySettingService;

    @Autowired
    RewardService rewardService;

    @Autowired
    DeliveryInfoService deliveryInfoService;

    @Autowired
    OrderDishDetailService orderDishDetailService;

    @Autowired
    DefaultPaycardService defaultPaycardService;

    @Autowired
    OrderPayInfoService orderPayInfoService;

    @Autowired
    DishService dishService;

    @Autowired
    DishDictService dishDictService;

    @Autowired
    CouponDictService couponDictService;

    @Autowired
    ReceiveOrderService cdos;

    @ApiOperation(value = "get user default delivery info")
    @GetMapping("default/delivery")
    public ResponseEntity<JsonResponse> getDefaultDeliveryInfo(Principal principal) {
	DeliverInfoDto diDto = orderService.getDefaultDeliverInfo(principal.getName());
	if (diDto != null)
	    return success("default deliver info", diDto);
	return notFound();
    }

    @ApiOperation(value = "get user default paycard info")
    @GetMapping("default/paycard")
    public ResponseEntity<JsonResponse> getDefaultPaycardInfo(Principal principal) throws AppException {
	CreditInfoDto ciDto = defaultPaycardService.getDefaultPaycardInfo(principal.getName());
	if (ciDto != null)
	    return success("default paycard info", ciDto);
	return notFound();
    }

    @ApiOperation(value = "create user default paycared info")
    @PostMapping("default/paycard")
    public ResponseEntity<JsonResponse> setDefaultPaycardInfo(@RequestBody CreditInfoDto creditInfoDto,
	    Principal principal) throws AppException {
	return success("Added", defaultPaycardService.saveDefaultPaycard(principal.getName(), creditInfoDto));

    }

    @ApiOperation(value = "get user history")
    @GetMapping("orderHistory by username")
    public ResponseEntity<JsonResponse> getHistoryOrder(String username) {
	List historyOrder = orderService.getOrderByUsername(username);
	if (!historyOrder.isEmpty()) {
	    return (success("orderHistory", historyOrder));
	} else {
	    return notFound();
	}
    }


    @ApiOperation(value = "Quick Checkout for User's Order")
    @PutMapping("order/quick-checkout/{preOrderId}")
    public ResponseEntity<JsonResponse> quickReCheckout(Principal principal, @PathVariable Integer preOrderId)
	    throws AppException {
	String username = principal.getName();
	Order preOrder = orderService.getOrderById(preOrderId);

	LOGGER.info(preOrder.getUsername());
	LOGGER.info(username);

	if (preOrder == null || !username.equals(preOrder.getUsername()))
	    return notFound();

	List<OrderDishDetail> detailList = new ArrayList<OrderDishDetail>();
	Param param = new Param();
	param.orderDetailList = new ArrayList<OrderDetailDto>();

	List<OrderDishDetail> preList = orderDishDetailService.getDishDetail(preOrderId);
	for (OrderDishDetail odd : preList) {
	    Dish dish = dishService.getDish(preOrder.getBranchId(), odd.getDishId());
	    DishDict dishDict = dishDictService.findDishDict(odd.getDishId());
	    short inventory = (short) (dish.getInventoryQuantity() - odd.getOrderQuantity());
	    if (inventory < 0) {
		return runOutOfDishes(dishDict.getName(), dish.getInventoryQuantity());
	    }
	    dish.setInventoryQuantity(inventory);
	    dishService.createDish(dish);
	    detailList.add(odd);
	    param.orderDetailList.add(DishMapper.toOrderDetailDto(odd, dish, dishDict));
	}

	Order order = new Order(username, preOrder.getBranchId(), new Date(), preOrder.getTotalPrice(),
		preOrder.getIsDeliver());
	orderService.createOrder(order);

	Integer orderId = order.getOrderId();
	for (OrderDishDetail odd : detailList) {
	    odd.setOrderId(orderId);
	    orderDishDetailService.create(odd);
	}

	param.branchId = preOrder.getBranchId();
	DeliveryInfo preDi = deliveryInfoService.getDeliveryInfo(preOrderId);
	if (preDi != null) {
	    DeliveryInfo di = new DeliveryInfo();
	    di.setOrderId(orderId);
	    di.setCity(preDi.getCity());
	    di.setStreet(preDi.getStreet());
	    di.setState(preDi.getState());
	    di.setZipcode(preDi.getZipcode());
	    di.setReceiverName(preDi.getReceiverName());
	    di.setPhone(preDi.getPhone());
	    deliveryInfoService.create(di);
	    param.isDelivery = true;
	    param.diDto = DeliverInfoMapper.toDto(di);
	}

	OrderPayInfo preOpi = orderPayInfoService.getOrderPayInfo(preOrderId);
	if (preOpi != null) {
	    OrderPayInfo opi = new OrderPayInfo();
	    opi.setOrderId(orderId);
	    opi.setCardholderName(preOpi.getCardholderName());
	    opi.setCardNum(preOpi.getCardNum());
	    opi.setCardType(preOpi.getCardType());
	    opi.setDate(new Date());
	    orderPayInfoService.create(opi);
	    param.ciDto = CreditInfoMapper.toDto(opi);
	}

	return success("Quick Checkout", param);
    }

    @ApiOperation(value = "Get Order History")
    @GetMapping("order/history/{orderId}")
    public ResponseEntity<JsonResponse> getOrderHistById(@PathVariable Integer orderId) {

	OrderHistoryDto orderHistoryDto = orderService.getHistoryOrderDto(orderId);

	if (orderHistoryDto == null)
	    notFound();

	return success("order", orderHistoryDto);
    }

    /// reCheckOut Order
    // @ApiOperation(value = "Check out for user's oreder")
    //@PostMapping("order/ReCheckout")
    public ResponseEntity<JsonResponse> reCheckout(@RequestBody Param param, Principal principal, int orderid) {
		String username = principal.getName();
		Short branchId = param.branchId;
		Float totalPrice = 0.00f;
		List<OrderDishDetail> detailList = new ArrayList<OrderDishDetail>();
		for (OrderDetailDto odDto : param.orderDetailList) {
			// Check whether we have enough inventory.
			Dish dish = dishService.getDish(branchId, odDto.getDishId());
			short inventory = (short) (dish.getInventoryQuantity() - odDto.getOrderQuantity());
			if (inventory < 0) {
				return runOutOfDishes(odDto.getDishName(), dish.getInventoryQuantity());
			}
			dish.setInventoryQuantity(inventory);
			dishService.updateDish(dish);

			totalPrice += odDto.getPrice() * odDto.getOrderQuantity();
			OrderDishDetail odd = new OrderDishDetail();
			odd.setDishId(odDto.getDishId());
			odd.setOrderQuantity(odDto.getOrderQuantity());
			detailList.add(odd);
		}

		if (param.isDelivery) {
			totalPrice += deliverySettingService.retrieveDeliverSetting(branchId).getFee();
		}

		if (param.usingCoupon) {
			Reward reward = rewardService.getValidCoupon(param.couponId);
			if (reward != null) {
				totalPrice -= couponDictService.getCouponInfo(param.couponId).getValue();
				rewardService.DeleteUsedCoupon(reward);
			} else {
				return noValidCoupon();
			}
		}
		List list11 = orderService.getInfoByID1(orderid);
		List list12 = orderService.getInfoByID2(orderid);
		OrderHistoryDto dto = orderService.getHistoryOrderDto(orderid);

		Order order = new Order(username, branchId, new Date(), totalPrice, param.isDelivery);
		orderService.createOrder(order);

		Integer orderId = order.getOrderId();
		for (OrderDishDetail odd : detailList) {
			odd.setOrderId(orderId);
			orderDishDetailService.create(odd);
		}

		if (param.isDelivery) {
			DeliveryInfo di = new DeliveryInfo();
			di.setOrderId(orderId);
			if (param.isDefaultAddress) {
				// DeliverInfoDto diDto =
				// orderService.getDefaultDeliverInfo(username);
				di.setReceiverName(dto.getReceiverName());
				di.setStreet(dto.getStreet());
				di.setCity(dto.getCity());
				di.setState(dto.getState());
				di.setZipcode(dto.getZipcode());
				di.setPhone(dto.getPhone());
			} else {
				di.setReceiverName(param.diDto.getReceiverName());
				di.setStreet(param.diDto.getStreet());
				di.setCity(param.diDto.getCity());
				di.setState(param.diDto.getState());
				di.setZipcode(param.diDto.getZipcode());
				di.setPhone(param.diDto.getPhone());
			}
			deliveryInfoService.create(di);
		}

		OrderPayInfo opi = new OrderPayInfo();
		opi.setOrderId(orderId);
		if (param.isDefaultPaycard) {

			opi.setCardholderName(dto.getCardholderName());
			opi.setCardNum(dto.getCardNum());
			opi.setCardType(dto.getCardType());
			opi.setDate(dto.getDate());
		} else {
			opi.setCardholderName(param.ciDto.getCardholderName());
			opi.setCardNum(param.ciDto.getCardNum());
			opi.setCardType(param.ciDto.getCardType());
			opi.setDate(param.ciDto.getDate());
		}
		orderPayInfoService.create(opi);
		return success("checkout the order", true);
	}

    @ApiOperation(value = "Check out for user's oreder")
    @PostMapping("order/checkout")
    public ResponseEntity<JsonResponse> checkout(@RequestBody Param param, Principal principal) throws AppException {
	String username = principal.getName();
	Short branchId = param.branchId;
	Float totalPrice = 0.00f;
	List<OrderDishDetail> detailList = new ArrayList<OrderDishDetail>();
	for (OrderDetailDto odDto : param.orderDetailList) {
	    // Check whether we have enough inventory.
	    Dish dish = dishService.getDish(branchId, odDto.getDishId());
	    short inventory = (short) (dish.getInventoryQuantity() - odDto.getOrderQuantity());
	    if (inventory < 0) {
		return runOutOfDishes(odDto.getDishName(), dish.getInventoryQuantity());
	    }
	    dish.setInventoryQuantity(inventory);
	    dishService.updateDish(dish);

	    totalPrice += odDto.getPrice() * odDto.getOrderQuantity();
	    OrderDishDetail odd = new OrderDishDetail();
	    odd.setDishId(odDto.getDishId());
	    odd.setOrderQuantity(odDto.getOrderQuantity());
	    detailList.add(odd);
	}

	if (param.isDelivery) {
	    totalPrice += deliverySettingService.retrieveDeliverSetting(branchId).getFee();
	}

	if (param.usingCoupon) {
	    Reward reward = rewardService.getValidCoupon(param.couponId);
	    if (reward != null) {
		totalPrice -= couponDictService.getCouponInfo(param.couponId).getValue();
		rewardService.DeleteUsedCoupon(reward);
	    } else {
		return noValidCoupon();
	    }
	}

	Order order = new Order(username, branchId, new Date(), totalPrice, param.isDelivery);
	orderService.createOrder(order);

	Integer orderId = order.getOrderId();
	for (OrderDishDetail odd : detailList) {
	    odd.setOrderId(orderId);
	    orderDishDetailService.create(odd);
	}

	if (param.isDelivery) {
	    DeliveryInfo di = new DeliveryInfo();
	    di.setOrderId(orderId);
	    if (param.isDefaultAddress) {
		DeliverInfoDto diDto = orderService.getDefaultDeliverInfo(username);
		di.setReceiverName(diDto.getReceiverName());
		di.setStreet(diDto.getStreet());
		di.setCity(diDto.getCity());
		di.setState(diDto.getState());
		di.setZipcode(diDto.getZipcode());
		di.setPhone(diDto.getPhone());
	    } else {
		di.setReceiverName(param.diDto.getReceiverName());
		di.setStreet(param.diDto.getStreet());
		di.setCity(param.diDto.getCity());
		di.setState(param.diDto.getState());
		di.setZipcode(param.diDto.getZipcode());
		di.setPhone(param.diDto.getPhone());
	    }
	    deliveryInfoService.create(di);
	}

	OrderPayInfo opi = new OrderPayInfo();
	opi.setOrderId(orderId);
	if (param.isDefaultPaycard) {
	    CreditInfoDto ciDto = defaultPaycardService.getDefaultPaycardInfo(username);
	    opi.setCardholderName(ciDto.getCardholderName());
	    opi.setCardNum(ciDto.getCardNum());
	    opi.setCardType(ciDto.getCardType());
	    opi.setDate(ciDto.getDate());
	} else {
	    opi.setCardholderName(param.ciDto.getCardholderName());
	    opi.setCardNum(param.ciDto.getCardNum());
	    opi.setCardType(param.ciDto.getCardType());
	    opi.setDate(param.ciDto.getDate());
	}
	orderPayInfoService.create(opi);

	return success("checkout the order", true);
    }

    @ApiOperation(value = "Confirm receive user's order", response = JsonResponse.class)
    @PostMapping("confirm" + "/order" + "/{orderId}")
    public ResponseEntity<JsonResponse> confirmReceiveOrder(@PathVariable Integer orderId, Principal principal) {

	Order confirmOrder = cdos.confirmReceiveOrder(orderId,principal.getName());
	if (confirmOrder != null)
	    return success("confirmed", confirmOrder);
	return notFound();
    }

    @ApiOperation(value = "show unreceived orders", response = JsonResponse.class)
    @GetMapping("customer" + "/{username}" + "/unreceived")
    public ResponseEntity<JsonResponse> unreceivedOrder(@PathVariable String username) {
	List<Order> unreceived = cdos.showNonReceivedOrder(username);
	if (unreceived != null)
	    return success("unreceived", unreceived);
	return badRequest("order all received by user " + username);
    }
}
