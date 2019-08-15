package cmpe.dos.entity;

import javax.persistence.*;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.util.Date;

import javax.persistence.*;

@Entity
public class Reward {

	@Id
	@Column(name = "reward_id")
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Integer rewardId;

	@Column(name = "coupon_id")
	private String couponId;

	@Column(length = 20)
	private String username;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
	@Temporal(TemporalType.DATE)

	@Column(name = "valid_start")
	private Date validStart;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
	@Temporal(TemporalType.DATE)
	@Column(name = "valid_end")
	private Date validEnd;


	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
	@Temporal(TemporalType.DATE)
	@Column(name = "used_date")
	private Date used_date;

	public Reward() {
		super();
	}

	public Integer getRewardId() {
		return rewardId;
	}

	public void setRewardId(Integer rewardId) {
		this.rewardId = rewardId;
	}

	public String getCouponId() {
		return couponId;
	}

	public void setCouponId(String couponId) {
		this.couponId = couponId;
	}

	public Date getValidStart() {
		return validStart;
	}

	public void setValidStart(Date validStart) {
		this.validStart = validStart;
	}

	public Date getValidEnd() {
		return validEnd;
	}

	public void setValidEnd(Date validEnd) {
		this.validEnd = validEnd;
	}

	public Date getUsed_date() {
		return used_date;
	}

	public void setUsed_date(Date used_date) {
		this.used_date = used_date;
	}

	@Override
	public String toString() {
		return "Reward [rewardId=" + rewardId + ", couponId=" + couponId + ", validStart=" + validStart + ", validEnd="
				+ validEnd + ", used_date=" + used_date + "]" + "username=" + username;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}
}
