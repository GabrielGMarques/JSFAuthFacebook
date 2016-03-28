package model.entity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class User {

	private String id;
	private String name;
	private String email;
	private String profileImageUrl;
	private Integer age;
	private Date birthday;
	private Locale locale;
	private String location;
	private String gender;
	private List<Institute> instiutes;

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public String getProfileImageUrl() {
		return profileImageUrl;
	}

	public void setProfileImageUrl(String profileImageUrl) {
		this.profileImageUrl = profileImageUrl;
	}

	public String getBirthdayAsString() {
		return new SimpleDateFormat("yyyy/MM/dd").format(birthday);
	}

	public void setBirthday(Date birthday) {
		this.birthday = birthday;
	}

	public String getLocaleName() {
		return locale.getDisplayName();
	}

	public void setLocale(Locale locale) {
		this.locale = locale;
	}

	public Integer getAge() {
		return age;
	}

	public void setAge(Integer age) {
		this.age = age;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getGender() {
		return gender;
	}

	public void setGender(String gender) {
		this.gender = gender;
	}

	public List<Institute> getInstiutes() {
		return instiutes;
	}

	public void setInstiutes(List<Institute> instiutes) {
		this.instiutes = instiutes;
	}

}
