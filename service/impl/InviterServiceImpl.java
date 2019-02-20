package com.liyunet.service.impl;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter.Indenter;
import com.liyunet.common.constant.Constant;
import com.liyunet.common.constant.DateHelper;
import com.liyunet.common.password.AES;
import com.liyunet.common.password.MD5;
import com.liyunet.common.password.MD5Util;
import com.liyunet.common.pushToken.PushAuthHelper;
import com.liyunet.common.util.DemandNumFactory;
import com.liyunet.common.util.IpResourceLocation;
import com.liyunet.common.util.TokenUtil;
import com.liyunet.common.util.UrlLoad;
import com.liyunet.domain.*;
import com.liyunet.domain.bet.BetUserInfo;
import com.liyunet.domain.gameapi.GameApiDemandOrder;
import com.liyunet.domain.gameapi.GameApiDemandOrderExample;
import com.liyunet.domain.gameapi.GameApiExchangeRate;
import com.liyunet.domain.gameapi.GameApiInfo;
import com.liyunet.domain.gameapi.GameApiInfoExample;
import com.liyunet.domain.gameapi.GameApiInfoExample.Criteria;
import com.liyunet.domain.gameapi.GameApiRoleInfo;
import com.liyunet.domain.gameapi.GameApiRoleInfoExample;
import com.liyunet.domain.gameapi.OrderVo;
import com.liyunet.domain.inviter.InviterBalance;
import com.liyunet.domain.inviter.InviterInfo;
import com.liyunet.domain.inviter.InviterInfoExample;
import com.liyunet.domain.inviter.InviterListVo;
import com.liyunet.exception.ServiceException;
import com.liyunet.mapper.game_api.GameApiMapper;
import com.liyunet.mapper.gameapiMapper.GameApiDemandOrderMapper;
import com.liyunet.mapper.gameapiMapper.GameApiExchangeRateMapper;
import com.liyunet.mapper.gameapiMapper.GameApiInfoMapper;
import com.liyunet.mapper.gameapiMapper.GameApiRoleInfoMapper;
import com.liyunet.mapper.inviterMapper.InviterBalanceMapper;
import com.liyunet.mapper.inviterMapper.InviterInfoMapper;
import com.liyunet.mapper.userMapper.CertificationMapper;
import com.liyunet.service.GameApiService;
import com.liyunet.service.InviterService;
import com.liyunet.util.PushRefinedCalculation;
import com.liyunet.vo.user.LuckyUserInfoVo;
import com.liyunet.vo.user.PushUserInfoVo;
import com.liyunet.vo.user.UserInfoVo;
import com.sun.mail.util.UUDecoderStream;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.Cookie;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * 邀请接口
 */

@Service("inviterService")
@Transactional
public class InviterServiceImpl implements InviterService {

	@Autowired
	private InviterInfoMapper im;
	@Autowired
	private InviterBalanceMapper bm;
	@Autowired
	private CertificationMapper cm;

	// 邀请注册
	@Override
	public Object inviter(Integer userId, String code) {
		// TODO Auto-generated method stub
		String formatedDateStr = DateHelper.getFormatedDateStr(new Date(), "yyyy-MM-dd HH:mm:ss");
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		// 根据code（区块id）查用户信息
		List<UserInfo> userInfoByid = cm.getUserInfoByUserAccount(Integer.parseInt(code));
		if (userInfoByid == null || userInfoByid.size() <= 0) {
			throw ServiceException.userException("", "邀请码错误");
		}
		InviterInfoExample example = new InviterInfoExample();
		com.liyunet.domain.inviter.InviterInfoExample.Criteria createCriteria = example.createCriteria();
		createCriteria.andInviterGuestEqualTo(userId);
		List<InviterInfo> selectByExample = im.selectByExample(example);
		if (selectByExample != null && selectByExample.size() > 0) {
			throw ServiceException.userException("", "已经被邀请");
		}
		List<UserInfo> uglist = cm.getUserInfoByid(userId + "");
		UserInfo userInfo = userInfoByid.get(0);
		InviterInfo record = new InviterInfo();
		record.setInviterMaster(userInfo.getId());
		record.setCode(code);
		record.setInviterGuest(userId);
		record.setLoginStatus(0);
		record.setCreatetime(formatedDateStr);
		record.setPhoneNum(uglist.get(0).getPhoneNum());
		int insertSelective = im.insertSelective(record);
		return insertSelective;
	}

	// 我的收益
	@Override
	public Object myPrifit(Integer userId) {
		// TODO Auto-generated method stub
		// 查实际收益个数
		int count = im.selectCountPrifit(userId);
		// 查条件全满足收益
		String banlance = getBanlance(count).split("_")[0];
		String power = getBanlance(count).split("_")[1];
		// 查总人数
		int peoplecount = im.selectCount(userId);
		// 用户实际收益
		String userbalance = PushRefinedCalculation.add(Double.parseDouble(banlance),
				PushRefinedCalculation.mul(50, peoplecount - count)) + "";
		// 预计人数
		int NoLogincount = im.selectCountPrifitNoLogin(userId);
		String NoLogibanlance = getBanlance(NoLogincount).split("_")[0];
		String NoLogipower = getBanlance(NoLogincount).split("_")[1];
		// 查登录
		InviterInfoExample example = new InviterInfoExample();
		com.liyunet.domain.inviter.InviterInfoExample.Criteria createCriteria = example.createCriteria();
		createCriteria.andInviterGuestEqualTo(userId);
		List<InviterInfo> selectByExample = im.selectByExample(example);
		// ---------------------
		// InviterInfoExample example2 = new InviterInfoExample();
		// com.liyunet.domain.inviter.InviterInfoExample.Criteria
		// createCriteria2 = example2.createCriteria();
		// createCriteria2.andInviterMasterEqualTo(userId);
		// List<InviterInfo> selectByExample2 = im.selectByExample(example2);

		if (selectByExample.size() > 0 && selectByExample != null) {
			if (selectByExample.get(0).getLoginStatus().intValue() == 1) {
				
				if(NoLogincount>0){
				NoLogibanlance = PushRefinedCalculation.add(Double.parseDouble(NoLogibanlance), 30) + "";
				NoLogipower = PushRefinedCalculation.add(Double.parseDouble(NoLogipower), 1) + "";
			}
				if (count > 0) {
					userbalance = PushRefinedCalculation.add(Double.parseDouble(userbalance), 30) + "";
					power = PushRefinedCalculation.add(Double.parseDouble(power), 1) + "";
				}

			}
		}
		Double noLogibanlance = Double.parseDouble(NoLogibanlance);
		Double noLogipower = Double.parseDouble(NoLogipower);
		Double balance = Double.parseDouble(userbalance);
		Double p = Double.parseDouble(power);
		Map<String, String> map = new HashMap<String, String>();
		map.put("AlreadyInviter", NoLogincount+"");
		map.put("EstimateProfit", noLogibanlance.intValue()+"");
		map.put("EstimatePower", noLogipower.intValue()+"");
		map.put("ActualProfit", balance.intValue()+"");
		map.put("ActualPower", p.intValue()+"");
		map.put("ActualPeople", count + "");

		return map;
	}

	public static String getBanlance(Integer count) {
		// TODO Auto-generated method stub
		// 邀请第1-2个人，每个邀请的收益为150 BIDT+1算力； 300 2
		//
		// 邀请第3-5个人，每个邀请的收益为200 BIDT+2算力；600 6
		//
		// 邀请第6-15个人，每个邀请的收益为280 BIDT+2算力；2800 20
		//
		// 邀请第16-30个人，每个邀请的收益为350 BIDT+2算力；5250 30
		//
		// 邀请第31-50个人，每个邀请的收益为400 BIDT+3算力；8000 60
		//
		// 邀请第51-100个人，每个邀请的收益为450 BIDT+3算力；22500 150
		//
		// 邀请第101-200个人，每个邀请的收益为500 BIDT+3算力； 50000 300
		//
		// 邀请第201-500个人，每个邀请的收益为550 BIDT+5算力。165000 1500
		// 查实际收益个数
		// Integer count = im.selectCountPrifit(userId);
		String balance = "";
		String prifit = "";
		if (count.intValue() <= 0) {
			balance = "0";
			prifit = "0";
		} else if (count.intValue() > 0 && count.intValue() <= 2) {
			balance = count * 150 + "";
			prifit = count * 1 + "";
		} else if (count.intValue() > 2 && count.intValue() <= 5) {
			balance = 300 + (count - 2) * 200 + "";
			prifit = 2 + (count - 2) * 2 + "";
		} else if (count.intValue() > 5 && count.intValue() <= 15) {
			balance = 300 + 600 + (count - 5) * 280 + "";
			prifit = 2 + 6 + (count - 5) * 2 + "";
		} else if (count.intValue() > 15 && count.intValue() <= 30) {
			balance = 300 + 600 + 2800 + (count - 15) * 350 + "";
			prifit = 2 + 6 + 20 + (count - 15) * 2 + "";
		} else if (count.intValue() > 30 && count.intValue() <= 50) {
			balance = 300 + 600 + 2800 + 5250 + (count - 30) * 400 + "";
			prifit = 2 + 6 + 20 + 30 + (count - 30) * 3 + "";
		} else if (count.intValue() > 50 && count.intValue() <= 100) {
			balance = 300 + 600 + 2800 + 5250 + 8000 + (count - 50) * 450 + "";
			prifit = 2 + 6 + 20 + 30 + 60 + (count - 50) * 3 + "";
		} else if (count.intValue() > 100 && count.intValue() <= 200) {
			balance = 300 + 600 + 2800 + 5250 + 8000 + 22500 + (count - 100) * 500 + "";
			prifit = 2 + 6 + 20 + 30 + 60 + 150 + (count - 100) * 3 + "";
		} else if (count.intValue() > 200 && count.intValue() <= 500) {
			balance = 300 + 600 + 2800 + 5250 + 8000 + 22500 + 50000 + (count - 200) * 550 + "";
			prifit = 2 + 6 + 20 + 30 + 60 + 150 + 300 + (count - 200) * 5 + "";
		} else {
			balance = 300 + 600 + 2800 + 5250 + 8000 + 22500 + 50000 + 165000 + "";
			prifit = 2 + 6 + 20 + 30 + 60 + 150 + 300 + 1500 + "";
		}
		return balance + "_" + prifit;
	}

	// 邀请记录
	@Override
	public Object inviterHistoryList(Integer userId) {
		// TODO Auto-generated method stub
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		InviterInfoExample example = new InviterInfoExample();
		example.setOrderByClause("createtime desc");
		com.liyunet.domain.inviter.InviterInfoExample.Criteria createCriteria = example.createCriteria();
		createCriteria.andInviterMasterEqualTo(userId);
		List<InviterInfo> selectByExample = im.selectByExample(example);
		int a = 0;
		List<InviterListVo> vos = new ArrayList<InviterListVo>();
		for (int i = 0; i < selectByExample.size(); i++) {
			String account = selectByExample.get(i).getPhoneNum();
			boolean status = account.contains("@");
			if (status) {
				int indexOf = account.indexOf("@");
				String substring2 = account.substring(indexOf - 2, account.length());
				String substring1 = account.substring(0, indexOf - 6);
				account = substring1 + "****" + substring2;
				System.out.println("包含");
			} else {
				account = account.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2");
			}

			InviterListVo vo = new InviterListVo();
			vo.setPhoneNum(account);
			vo.setCreatetime(selectByExample.get(i).getCreatetime().split(" ")[0]);
			if (selectByExample.get(i).getLoginStatus().intValue() == 0) {
				vo.setPrifit("0");
				vo.setTips("0");
			}
			if (selectByExample.get(i).getLoginStatus().intValue() == 1) {
				int count = im.selectCount(selectByExample.get(i).getInviterGuest());
				if (count > 0) {
					a = a + 1;
					if (a <= 2) {
						vo.setPrifit("150");
						vo.setPower("1");
					} else if (a > 2 && a <= 5) {
						vo.setPrifit("200");
						vo.setPower("2");
					} else if (a > 5 && a <= 15) {
						vo.setPrifit("280");
						vo.setPower("2");
					} else if (a > 15 && a <= 30) {
						vo.setPrifit("350");
						vo.setPower("2");
					} else if (a > 30 && a <= 50) {
						vo.setPrifit("400");
						vo.setPower("3");
					} else if (a > 50 && a <= 100) {
						vo.setPrifit("450");
						vo.setPower("3");
					} else if (a > 100 && a <= 200) {
						vo.setPrifit("500");
						vo.setPower("3");
					} else if (a > 200 && a <= 500) {
						vo.setPrifit("550");
						vo.setPower("5");
					} else {
						vo.setPrifit("550");
						vo.setPower("5");
					}
					vo.setTips("2");

				} else {
					vo.setPrifit("50");
					vo.setPower("0");
					vo.setTips("1");
				}

			}

			vos.add(vo);

		}

		return vos;
	}

	// 查余额
	@Override
	public Object getMyBalance(Integer userId) {
		// TODO Auto-generated method stub
		// 查实际收益个数
		int count = im.selectCountPrifit(userId);
		// 查条件全满足收益
		String banlance = getBanlance(count).split("_")[0];
		String power = getBanlance(count).split("_")[1];
		// 查总人数
		int peoplecount = im.selectCount(userId);
		// 用户实际收益
		String userbalance = PushRefinedCalculation.add(Double.parseDouble(banlance),
				PushRefinedCalculation.mul(50, peoplecount - count)) + "";
		InviterInfoExample example = new InviterInfoExample();
		com.liyunet.domain.inviter.InviterInfoExample.Criteria createCriteria = example.createCriteria();
		createCriteria.andInviterGuestEqualTo(userId);
		List<InviterInfo> selectByExample = im.selectByExample(example);

		if (selectByExample.size() > 0 && selectByExample != null) {
			if (selectByExample.get(0).getLoginStatus().intValue() == 1) {
				if (count > 0) {
					userbalance = PushRefinedCalculation.add(Double.parseDouble(userbalance), 30) + "";
				}

			}
		}
		
		
		
		
		// 提现记录
		String selectBalance = bm.selectBalance(userId);
		if (selectBalance == null || selectBalance.length() <= 0) {
			selectBalance = "0";
		}
		Double a = PushRefinedCalculation.sub(Double.parseDouble(userbalance), Double.parseDouble(selectBalance));
		return a.intValue();
	}

	// 提现
	@Override
	public Object cashWithdrawal(Integer userId, String bidtnum) {
		// TODO Auto-generated method stub
		Double parseDouble = Double.parseDouble(bidtnum);
		if (parseDouble <= 0) {
			throw ServiceException.userException("", "提现金额不能小于等于0");
		}

		if (parseDouble < 500) {
			throw ServiceException.userException("", "提现金额不能小于500");
		}
		String s = null;
		String timestampStr = System.currentTimeMillis() + "";
		double mul = PushRefinedCalculation.mul(10, parseDouble);
		try {
			String encode1 = URLEncoder.encode(AES.AESEncode("LYWH@#$!32", userId + ""), "UTF-8");
			String encode3 = URLEncoder.encode(AES.AESEncode("9$dz4Y33oy", mul + ""), "UTF-8");
			String encode4 = URLEncoder.encode(AES.AESEncode("9$dz4Y33oy", "yobXT2ENRb" + timestampStr), "UTF-8");
			s = UrlLoad.load(IpResourceLocation.TT_EGGWORLD_IP + "/ttc-eggworld/ttc/record",
					"openId=" + encode1 + "&muchMoney=" + encode3 + "&transactionType=43"
							+ "&app_id=3360a88e4318896c9fe3e031e64541f9c176af67fa87634f&mch_id=yobXT2ENRb&timestampStr="
							+ timestampStr + "&sign=" + encode4);

		} catch (Exception e) {
			e.printStackTrace();
		}
		JSONObject jsonObject = JSON.parseObject(s);
		String string = jsonObject.getString("state");
		JSONObject jsonObject1 = JSON.parseObject(string);
		String code = jsonObject1.getString("code");
		if ("20000".equals(code)) {

			// 如果20000
			String formatedDateStr = DateHelper.getFormatedDateStr(new Date(), "yyyy-MM-dd HH:mm:ss");
			InviterBalance balance = new InviterBalance();
			balance.setBalance(bidtnum);
			balance.setCreatetime(formatedDateStr);
			balance.setState("1");
			balance.setUserid(userId);
			bm.insertSelective(balance);

		} else if ("20002".equals(code)) {

			String msg = jsonObject1.getString("msg");
			if (msg.equals("success")) {
				throw ServiceException.userException("", "未完成kyc二级认证");
			} else {
				throw ServiceException.userException("", msg);
			}
		}

		return null;
	}
public static void main(String[] args) {
	Double d = new Double(2);
	int i = d.intValue();
	System.out.println(i);
}
}
