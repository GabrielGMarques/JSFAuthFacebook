package model.service;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.brickred.socialauth.exception.ServerDataException;
import org.brickred.socialauth.exception.SocialAuthException;
import org.brickred.socialauth.util.AccessGrant;
import org.brickred.socialauth.util.Constants;
import org.brickred.socialauth.util.HttpUtil;
import org.brickred.socialauth.util.MethodType;
import org.brickred.socialauth.util.Response;
import org.json.JSONException;
import org.json.JSONObject;

import controller.LoginBean;
import model.entity.Institute;
import model.entity.User;

public class FB0Auth {
	private static Map<String, String> endpoints;
	private AccessGrant accessGrant;

	static {
		endpoints = new HashMap<String, String>();
		endpoints.put(Constants.OAUTH_AUTHORIZATION_URL, "https://graph.facebook.com/oauth/authorize");
		endpoints.put(Constants.OAUTH_ACCESS_TOKEN_URL, "https://graph.facebook.com/oauth/access_token");
	}

	// Aqui define-se os atributos desejados para a sua classe
	private static final String PROFILE_FIELDS = "?fields=id,name,picture,age_range,birthday,email,first_name,last_name,about,gender,location,locale,education";
	private static final String PROFILE_URL = "https://graph.facebook.com/me" + PROFILE_FIELDS;

	public User authFacebookLogin() throws Exception {
		String presp;

		try {
			Response response = executeFeed(PROFILE_URL);
			presp = response.getResponseBodyAsString(Constants.ENCODING);
		} catch (Exception e) {
			throw new SocialAuthException("Error while getting profile from " + PROFILE_URL, e);
		}
		try {
			// Basta extrair da response um JSONObject e intepreta-lo como
			// quiser
			JSONObject resp = new JSONObject(presp);

			User user = new User();
			System.out.println(resp);
			user.setId(resp.getString("id"));
			user.setName((resp.getString("name")));
			user.setEmail(resp.getString("email"));
			user.setProfileImageUrl(resp.getJSONObject("picture").getJSONObject("data").getString("url"));
			user.setLocation(resp.getJSONObject("location").getString("name"));
			user.setLocale(new Locale(resp.getString("locale")));
			String birthDate = resp.getString("birthday");
			user.setBirthday(new SimpleDateFormat("dd/MM/yyyy").parse(birthDate));
			user.setGender(resp.getString("gender"));
			user.setAge(resp.getJSONObject("age_range").getInt("min"));

			// Pega as escolas do usuario
			String educationList = resp.get("education").toString();
			// Exclui os colchetes
			educationList = educationList.substring(1, educationList.length() - 1);
			JSONObject instituteFields = new JSONObject(educationList);
			List<Institute> institutes = new ArrayList<>();
			Institute institute = new Institute();
			institute.setName(instituteFields.getJSONObject("school").getString("name"));
			institute.setFinalYearOfCourse(instituteFields.getJSONObject("year").getString("name"));
			institutes.add(institute);
			user.setInstiutes(institutes);

			return user;

		} catch (Exception ex) {
			throw new ServerDataException("Failed to parse the user profile json : " + presp, ex);
		}
	}

	// Intepreta a url
	public void encode(String code, String methodType) throws Exception {

		String acode;
		String accessToken = null;
		try {
			acode = URLEncoder.encode(code, "UTF-8");
		} catch (Exception e) {
			acode = code;
		}
		StringBuffer sb = new StringBuffer();
		if (MethodType.GET.toString().equals(methodType)) {
			sb.append(endpoints.get(Constants.OAUTH_ACCESS_TOKEN_URL));
			char separator = endpoints.get(Constants.OAUTH_ACCESS_TOKEN_URL).indexOf('?') == -1 ? '?' : '&';
			sb.append(separator);
		}
		sb.append("client_id=").append(LoginBean.FACEBOOK_APP_ID);
		sb.append("&redirect_uri=").append(LoginBean.REDIRECT_TO);
		sb.append("&client_secret=").append(LoginBean.FACEBOOK_APP_SECRET);
		sb.append("&code=").append(acode);
		sb.append("&grant_type=authorization_code");

		Response response;
		String authURL = null;
		try {
			if (MethodType.GET.toString().equals(methodType)) {
				authURL = sb.toString();
				response = HttpUtil.doHttpRequest(authURL, methodType, null, null);

			} else {
				authURL = endpoints.get(Constants.OAUTH_ACCESS_TOKEN_URL);
				response = HttpUtil.doHttpRequest(authURL, methodType, sb.toString(), null);
			}
		} catch (Exception e) {
			throw new SocialAuthException("Error in url : " + authURL, e);
		}
		String result;
		try {
			result = response.getResponseBodyAsString(Constants.ENCODING);
		} catch (IOException io) {
			throw new SocialAuthException(io);
		}
		Map<String, Object> attributes = new HashMap<String, Object>();
		Integer expires = null;
		if (result.indexOf("{") < 0) {
			String[] pairs = result.split("&");
			for (String pair : pairs) {
				String[] kv = pair.split("=");
				if (kv.length != 2) {
					throw new SocialAuthException("Unexpected auth response from " + authURL);
				} else {
					if (kv[0].equals("access_token")) {
						accessToken = kv[1];
					} else if (kv[0].equals("expires")) {
						expires = Integer.valueOf(kv[1]);
					} else if (kv[0].equals("expires_in")) {
						expires = Integer.valueOf(kv[1]);
					} else {
						attributes.put(kv[0], kv[1]);
					}
				}
			}
		} else {
			try {
				JSONObject jObj = new JSONObject(result);
				if (jObj.has("access_token")) {
					accessToken = jObj.getString("access_token");
				}

				// expires_in can come in several different types, and newer
				// org.json versions complain if you try to do getString over an
				// integer...
				if (jObj.has("expires_in") && jObj.opt("expires_in") != null) {
					String str = jObj.get("expires_in").toString();
					if (str != null && str.length() > 0) {
						expires = Integer.valueOf(str);
					}
				}
				if (accessToken != null) {
					Iterator<String> keyItr = jObj.keys();
					while (keyItr.hasNext()) {
						String key = keyItr.next();
						if (!"access_token".equals(key) && !"expires_in".equals(key) && jObj.opt(key) != null) {
							attributes.put(key, jObj.opt(key).toString());
						}
					}
				}
			} catch (JSONException je) {
				throw new SocialAuthException("Unexpected auth response from " + authURL);
			}
		}
		if (accessToken != null) {
			accessGrant = new AccessGrant();
			accessGrant.setKey(accessToken);
			accessGrant.setAttribute(Constants.EXPIRES, expires);
			if (attributes.size() > 0) {
				accessGrant.setAttributes(attributes);
			}

			accessGrant.setProviderId(LoginBean.FACEBOOK_APP_ID);
		}
	}

	public Response executeFeed(final String url) throws Exception {
		if (accessGrant == null) {
			throw new SocialAuthException("Please call verifyResponse function first to get Access Token");
		}
		char separator = url.indexOf('?') == -1 ? '?' : '&';
		String urlStr = url + separator + Constants.ACCESS_TOKEN_PARAMETER_NAME + "=" + accessGrant.getKey();
		return HttpUtil.doHttpRequest(urlStr, MethodType.GET.toString(), null, null);
	}

}
