/*
 * Copyright 2011 buddycloud
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.buddycloud.pusher.utils;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Element;

import com.buddycloud.pusher.NotificationSettings;
import com.buddycloud.pusher.db.DataSource;

/**
 * @author Abmar
 *
 */
public class NotificationUtils {

	private static final Logger LOGGER = Logger.getLogger(NotificationUtils.class);
	
	public static List<String> getAllUsers(DataSource dataSource) {
		PreparedStatement statement = null;
		try {
			statement = dataSource.prepareStatement(
					"SELECT DISTINCT jid FROM notification_settings");
			ResultSet resultSet = statement.executeQuery();
			List<String> jids = new LinkedList<String>();
			while (resultSet.next()) {
				jids.add(resultSet.getString("jid"));
			}
			return jids;
		} catch (SQLException e) {
			LOGGER.error("Could not retrieve users.", e);
			throw new RuntimeException(e);
		} finally {
			DataSource.close(statement);
		}
	}
	
	public static List<NotificationSettings> getNotificationSettings(String jid, DataSource dataSource) {
		PreparedStatement statement = null;
		try {
			statement = dataSource.prepareStatement(
					"SELECT * FROM notification_settings WHERE jid=?", 
					jid);
			ResultSet resultSet = statement.executeQuery();
			List<NotificationSettings> allSettings = new LinkedList<NotificationSettings>();
			while (resultSet.next()) {
				allSettings.add(parseSettings(resultSet));
			}
			return allSettings;
		} catch (SQLException e) {
			LOGGER.error("Could not get notification settings from user [" + jid + "].", e);
			throw new RuntimeException(e);
		} finally {
			DataSource.close(statement);
		}
	}
	
	public static NotificationSettings getNotificationSettingsByType(String jid, String type, DataSource dataSource) {
		PreparedStatement statement = null;
		try {
			statement = dataSource.prepareStatement(
					"SELECT * FROM notification_settings WHERE jid=? AND type=?", 
					jid, type);
			ResultSet resultSet = statement.executeQuery();
			if (resultSet.next()) {
				return parseSettings(resultSet);
			}
			return null;
		} catch (SQLException e) {
			LOGGER.error("Could not get notification settings from user [" + jid + "].", e);
			throw new RuntimeException(e);
		} finally {
			DataSource.close(statement);
		}
	}

	private static NotificationSettings parseSettings(ResultSet resultSet)
			throws SQLException {
		NotificationSettings notificationSettings = new NotificationSettings();
		notificationSettings.setJid(resultSet.getString("jid"));
		notificationSettings.setTarget(resultSet.getString("target"));
		notificationSettings.setType(resultSet.getString("type"));
		notificationSettings.setPostAfterMe(resultSet.getBoolean("post_after_me"));
		notificationSettings.setPostMentionedMe(resultSet.getBoolean("post_mentioned_me"));
		notificationSettings.setPostOnMyChannel(resultSet.getBoolean("post_on_my_channel"));
		notificationSettings.setPostOnSubscribedChannel(resultSet.getBoolean("post_on_subscribed_channel"));
		notificationSettings.setFollowedMyChannel(resultSet.getBoolean("follow_my_channel"));
		notificationSettings.setFollowRequest(resultSet.getBoolean("follow_request"));
		return notificationSettings;
	}

	public static NotificationSettings updateNotificationSettings(String jid, String type, 
			DataSource dataSource, NotificationSettings notificationSettings) {
		
		NotificationSettings updatedNotificationsSettings = getNotificationSettingsByType(jid, type, dataSource);
		if (updatedNotificationsSettings == null) {
			updatedNotificationsSettings = notificationSettings;
		} else {
			NotificationUtils.updateNotificationSettings(notificationSettings, updatedNotificationsSettings);
		}
		
		PreparedStatement deleteStatement = null;
		PreparedStatement insertStatement = null;
		Connection connection = null;
		try {
			connection = dataSource.getConnection();
			connection.setAutoCommit(false);
			deleteStatement = dataSource.prepareStatement(
					"DELETE FROM notification_settings WHERE jid=? AND type=?", 
					connection, jid, type);
			deleteStatement.executeUpdate();
			
			insertStatement = dataSource.prepareStatement(
					"INSERT INTO notification_settings(jid, target, type, post_after_me, post_mentioned_me, post_on_my_channel, " +
					"post_on_subscribed_channel, follow_my_channel, follow_request) values (?, ?, ?, ?, ?, ?, ?, ?, ?)", connection, 
					jid, 
					updatedNotificationsSettings.getTarget(),
					updatedNotificationsSettings.getType(),
					updatedNotificationsSettings.getPostAfterMe(), 
					updatedNotificationsSettings.getPostMentionedMe(), 
					updatedNotificationsSettings.getPostOnMyChannel(), 
					updatedNotificationsSettings.getPostOnSubscribedChannel(), 
					updatedNotificationsSettings.getFollowedMyChannel(),
					updatedNotificationsSettings.getFollowRequest());
			insertStatement.executeUpdate();
			
			connection.commit();
			
			return updatedNotificationsSettings;
		} catch (SQLException e) {
			LOGGER.error("Could not update notification settings from user [" + jid + "].", e);
			if (connection != null) {
				try {
					connection.rollback();
				} catch (SQLException e1) {
					LOGGER.error("Could not rollback update notification settings operation.", e1);
				}
			}
			throw new RuntimeException(e);
		} finally {
			if (deleteStatement != null) {
				try {
					deleteStatement.close();
				} catch (SQLException e) {
					LOGGER.error(e);
				}
			}
			if (insertStatement != null) {
				try {
					insertStatement.close();
				} catch (SQLException e) {
					LOGGER.error(e);
				}
			}
			try {
				connection.close();
			} catch (SQLException e) {
				LOGGER.error(e);
			}
		}
	}

	private static void updateNotificationSettings(
			NotificationSettings notificationSettings,
			NotificationSettings oldNotificationsSettings) {
		
		if (notificationSettings.getTarget() != null) {
			oldNotificationsSettings.setTarget(
					notificationSettings.getTarget());
		}
		
		if (notificationSettings.getPostAfterMe() != null) {
			oldNotificationsSettings.setPostAfterMe(
					notificationSettings.getPostAfterMe());
		}
		if (notificationSettings.getPostMentionedMe() != null) {
			oldNotificationsSettings.setPostMentionedMe(
					notificationSettings.getPostMentionedMe());
		}
		if (notificationSettings.getPostOnMyChannel() != null) {
			oldNotificationsSettings.setPostOnMyChannel(
					notificationSettings.getPostOnMyChannel());
		}
		if (notificationSettings.getPostOnSubscribedChannel() != null) {
			oldNotificationsSettings.setPostOnSubscribedChannel(
					notificationSettings.getPostOnSubscribedChannel());
		}
		if (notificationSettings.getFollowedMyChannel() != null) {
			oldNotificationsSettings.setFollowedMyChannel(
					notificationSettings.getFollowedMyChannel());
		}
		
		if (notificationSettings.getFollowRequest() != null) {
			oldNotificationsSettings.setFollowRequest(
					notificationSettings.getFollowRequest());
		}
	}

	/**
	 * @param queryElement 
	 * @param notificationSettings
	 * @return
	 */
	public static void appendXML(Element queryElement, NotificationSettings notificationSettings) {
		Element settingsEl = queryElement.addElement("notificationSettings");
		if (notificationSettings == null) {
			return;
		}
		setText(settingsEl, "target", notificationSettings.getTarget());
		setText(settingsEl, "type", notificationSettings.getType());
		setText(settingsEl, "postAfterMe", notificationSettings.getPostAfterMe());
		setText(settingsEl, "postMentionedMe", notificationSettings.getPostMentionedMe());
		setText(settingsEl, "postOnMyChannel", notificationSettings.getPostOnMyChannel());
		setText(settingsEl, "postOnSubscribedChannel", 
				notificationSettings.getPostOnSubscribedChannel());
		setText(settingsEl, "followMyChannel", notificationSettings.getFollowedMyChannel());
		setText(settingsEl, "followRequest", notificationSettings.getFollowRequest());
	}
	
	private static void setText(Element rootEl, String property, Object text) {
		if (text == null) {
			return;
		}
		rootEl.addElement(property).setText(text.toString());
	}

	public static NotificationSettings fromXML(Element settingsEl) {
		NotificationSettings notificationSettings = new NotificationSettings();
		
		String target = getString(settingsEl, "target");
		String type = getString(settingsEl, "type");
		Boolean postAfterMe = getBoolean(settingsEl, "postAfterMe");
		Boolean postMentionedMe = getBoolean(settingsEl, "postMentionedMe");
		Boolean postOnMyChannel = getBoolean(settingsEl, "postOnMyChannel");
		Boolean postOnSubscribedChannel = getBoolean(settingsEl, "postOnSubscribedChannel");
		Boolean followMyChannel = getBoolean(settingsEl, "followMyChannel");
		Boolean followRequest = getBoolean(settingsEl, "followRequest");
		
		notificationSettings.setTarget(target);
		notificationSettings.setType(type);
		
		if (postAfterMe != null) {
			notificationSettings.setPostAfterMe(postAfterMe);
		}
		if (postMentionedMe != null) {
			notificationSettings.setPostMentionedMe(postMentionedMe);
		}
		if (postOnMyChannel != null) {
			notificationSettings.setPostOnMyChannel(postOnMyChannel);
		}
		if (postOnSubscribedChannel != null) {
			notificationSettings.setPostOnSubscribedChannel(postOnSubscribedChannel);
		}
		if (followMyChannel != null) {
			notificationSettings.setFollowedMyChannel(followMyChannel);
		}
		if (followRequest != null) {
			notificationSettings.setFollowRequest(followRequest);
		}
		return notificationSettings;
	}

	private static Boolean getBoolean(Element settingsEl, String key) {
		Element el = settingsEl.element(key);
		if (el == null) {
			return null;
		}
		return Boolean.valueOf(el.getText());
	}
	
	private static String getString(Element settingsEl, String key) {
		Element el = settingsEl.element(key);
		if (el == null) {
			return null;
		}
		return el.getText();
	}
}
