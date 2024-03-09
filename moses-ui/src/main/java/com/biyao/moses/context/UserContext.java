package com.biyao.moses.context;

/**
 * 
 * @Description
 * @Date 2018年9月27日
 */
public class UserContext implements AutoCloseable {

	public static final ThreadLocal<Object> CURRENT_USER = new ThreadLocal<>();

	public UserContext(ByUser user) {
		CURRENT_USER.set(user);
	}

	public static ByUser getUser() {
		Object user = CURRENT_USER.get();
		if (user instanceof ByUser) {
			return (ByUser) user;
		} else {
			return null;
		}
	}

	@Override
	public void close() {
		CURRENT_USER.remove();
	}

	public static void manulClose() {
		ByUser user = getUser();
		if (user==null) {
			return;
		}
		if (user.getAidMap() != null) {
			user.getAidMap().clear();
		}
		if (user.getTrackMap() != null) {
			user.getTrackMap().clear();
		}
		if (user.getRankTrackMap() != null) {
			user.getRankTrackMap().clear();
		}
		CURRENT_USER.remove();
	}
}
