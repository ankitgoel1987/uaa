package org.cloudfoundry.identity.uaa.authentication.manager;

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cloudfoundry.identity.uaa.authentication.AuthzAuthenticationRequest;
import org.cloudfoundry.identity.uaa.authentication.UaaAuthentication;
import org.cloudfoundry.identity.uaa.authentication.UaaAuthenticationDetails;
import org.cloudfoundry.identity.uaa.authentication.UaaPrincipal;
import org.cloudfoundry.identity.uaa.event.UserAuthenticationSuccessEvent;
import org.cloudfoundry.identity.uaa.scim.ScimUserBootstrap;
import org.cloudfoundry.identity.uaa.user.UaaAuthority;
import org.cloudfoundry.identity.uaa.user.UaaUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.common.util.RandomValueStringGenerator;
import org.springframework.security.oauth2.provider.OAuth2Authentication;

public class LoginAuthenticationManager implements AuthenticationManager, ApplicationEventPublisherAware {

	private final Log logger = LogFactory.getLog(getClass());

	private ApplicationEventPublisher eventPublisher;

	private ScimUserBootstrap scimUserBootstrap;

	boolean addNewAccounts = false;

	private RandomValueStringGenerator generator = new RandomValueStringGenerator();

	/**
	 * Flag to indicate that the scim user bootstrap (if provided) should be used to add new accounts when
	 * authenticated.
	 * 
	 * @param addNewAccounts the flag to set (default false)
	 */
	public void setAddNewAccounts(boolean addNewAccounts) {
		this.addNewAccounts = addNewAccounts;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher eventPublisher) {
		this.eventPublisher = eventPublisher;
	}

	/**
	 * If set this bootstrap helper will be used to register new accounts.
	 * 
	 * @param scimUserBootstrap the scim user bootstrap to set
	 */
	public void setScimUserBootstrap(ScimUserBootstrap scimUserBootstrap) {
		this.scimUserBootstrap = scimUserBootstrap;
	}

	@Override
	public Authentication authenticate(Authentication request) throws AuthenticationException {

		if (!(request instanceof AuthzAuthenticationRequest)) {
			return null;
		}

		AuthzAuthenticationRequest req = (AuthzAuthenticationRequest) request;
		Map<String, String> info = req.getInfo();

		logger.debug("Processing authentication request for " + req.getName());

		SecurityContext context = SecurityContextHolder.getContext();

		if (context.getAuthentication() instanceof OAuth2Authentication) {
			OAuth2Authentication authentication = (OAuth2Authentication) context.getAuthentication();
			if (authentication.isClientOnly()) {
				UaaUser user = getUser(req, info);
				if (scimUserBootstrap != null && addNewAccounts) {
					// Register new users automatically
					scimUserBootstrap.addUser(user);
				}
				Authentication success = new UaaAuthentication(new UaaPrincipal(user), UaaAuthority.USER_AUTHORITIES,
						(UaaAuthenticationDetails) req.getDetails());
				eventPublisher.publishEvent(new UserAuthenticationSuccessEvent(user, success));
				return success;
			}
		}

		logger.debug("Did not locate login credentials");
		throw new BadCredentialsException("Bad credentials");

	}

	protected UaaUser getUser(AuthzAuthenticationRequest req, Map<String, String> info) {
		String name = req.getName();
		String email = info.get("email");
		if (email == null) {
			if (name.contains("@")) {
				email = name;
			}
			else {
				email = name + "@unknown.org";
			}
		}
		String givenName = info.get("given_name");
		if (givenName == null) {
			givenName = email.split("@")[0];
		}
		String familyName = info.get("family_name");
		if (familyName == null) {
			familyName = email.split("@")[1];
		}
		return new UaaUser(name, generator.generate(), email, givenName, familyName);
	}

}
