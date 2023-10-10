package com.idega.jackrabbit.security;

import com.idega.presentation.IWContext;
import com.idega.user.data.bean.User;

public interface RepositoryItemAccessManager {

	public boolean hasPermission(IWContext iwc, String path, User user) throws Exception;

}