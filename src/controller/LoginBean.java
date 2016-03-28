package controller;

import java.io.Serializable;
import java.util.Properties;

import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ComponentSystemEvent;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.brickred.socialauth.Profile;
import org.brickred.socialauth.SocialAuthConfig;
import org.brickred.socialauth.SocialAuthManager;

import model.entity.User;
import model.service.FB0Auth;

@Named
@SessionScoped
public class LoginBean implements Serializable {

	private SocialAuthManager manager;
	private String originalURL;
	private String providerID;
	private User user;
	// O id do seu app
	public static final String FACEBOOK_APP_ID = "********";
	// A senha do seu app
	public static final String FACEBOOK_APP_SECRET = "*******";
	// O caminho onde ser√° feito o redirect
	public static final String REDIRECT_TO = "http://localhost:8080/JSF-Facebook/success.faces";

	public String socialConnect() throws Exception {
		try {
			// Define o id do seu aplicativo e a sua senha
			Properties props = System.getProperties();
			props.put("graph.facebook.com.consumer_key", FACEBOOK_APP_ID);
			props.put("graph.facebook.com.consumer_secret", FACEBOOK_APP_SECRET);
			// Define as permissıes da sua aplicaÁ„o
			props.put("graph.facebook.com.custom_permissions",
					"public_profile,user_education_history,publish_actions,user_managed_groups");

			// Inicia os componentes requiridos
			SocialAuthConfig config = SocialAuthConfig.getDefault();
			config.load(props);
			manager = new SocialAuthManager();
			manager.setSocialAuthConfig(config);

			// A pagina ser√° redirecionada para a success.faces onde irei
			// extrair os dados
			ExternalContext externalContext = FacesContext.getCurrentInstance().getExternalContext();
			String successURL = externalContext.getRequestContextPath() + "/success.xhtml";
			String authenticationURL = manager.getAuthenticationUrl(providerID,
					"http://localhost:8080/JSF-Facebook/success.faces");
			System.out.println(authenticationURL);
			FacesContext.getCurrentInstance().getExternalContext().redirect(authenticationURL);

		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
		}
		return "success.faces?redirect=true";
	}

	public void fillUser(ComponentSystemEvent event) {
		try {
			HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
					.getRequest();
			String parameter = request.getParameter("code");

			// Extrai o usuario
			if (parameter != null) {
				FB0Auth fb = new FB0Auth();
				// Intepreta da url
				fb.encode(parameter, "GET");
				// Extrai os atributos da url e retorna o objeto da classe user
				user = fb.authFacebookLogin();
			}
		} catch (Exception ex) {
			System.out.println("UserSession - Exception: " + ex.getMessage());
			ex.getCause().printStackTrace();
		}
	}

	public String getOriginalURL() {
		return originalURL;
	}

	public void setOriginalURL(String originalURL) {
		this.originalURL = originalURL;
	}

	public String getProviderID() {
		return providerID;
	}

	public void setProviderID(String providerID) {
		this.providerID = providerID;
	}

	public User getUser() {
		return user;
	}

	public void setUser(User user) {
		this.user = user;
	}

}
