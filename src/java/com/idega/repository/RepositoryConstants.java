package com.idega.repository;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.jackrabbit.commons.webdav.JcrRemotingConstants;

import com.idega.util.CoreConstants;

public class RepositoryConstants {

	public static final String	DAV_NAME_SPACE = "DAV:",

								ROLENAME_USERS = "users",
								ROLENAME_ROOT = "root",

								PATH_USERS = "/users",
								PATH_GROUPS = "/groups",
								PATH_ROLES = "/roles",
								PATH_ACTIONS = "/actions",

								SUBJECT_URI_ALL = "all",
								SUBJECT_URI_AUTHENTICATED = "authenticated",
								SUBJECT_URI_OWNER = "owner",
								SUBJECT_URI_SELF = "self",
								SUBJECT_URI_UNAUTHENTICATED = "unauthenticated",

								FOLDER_NAME_PUBLIC = "/public",
								FOLDER_NAME_SHARED = "/shared",
								FOLDER_NAME_DROPBOX = "/dropbox",

								PROPERTYNAME_CATEGORY = "categories",

								REPOSITORY = CoreConstants.REPOSITORY,

								DEFAULT_WORKSPACE_ROOT_CONTENT = CoreConstants.SLASH + CoreConstants.REPOSITORY_DEFAULT_WORKSPACE +
											JcrRemotingConstants.ROOT_ITEM_RESOURCEPATH;

	public static List<String> ALL_STANDARD_SUBJECT_URIS = Collections.unmodifiableList(Arrays.asList(
			SUBJECT_URI_ALL,
			SUBJECT_URI_AUTHENTICATED,
			SUBJECT_URI_OWNER,
			SUBJECT_URI_SELF,
			SUBJECT_URI_UNAUTHENTICATED
	));
}