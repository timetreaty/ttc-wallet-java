package com.liyunet.service;

import java.text.DecimalFormat;

public interface InviterService {
	// 邀请注册
	Object inviter(Integer userId, String code);

	// 我的收益
	Object myPrifit(Integer userId);

	// 邀请记录
	Object inviterHistoryList(Integer userId);

	// 查余额
	Object getMyBalance(Integer userId);

	// 提现
	Object cashWithdrawal(Integer userId, String bidtnum);

}
