/*
 * Cloud Foundry 2012.02.03 Beta
 * Copyright (c) [2009-2012] VMware, Inc. All Rights Reserved.
 *
 * This product is licensed to you under the Apache License, Version 2.0 (the "License").
 * You may not use this product except in compliance with the License.
 *
 * This product includes a number of subcomponents with
 * separate copyright notices and license terms. Your use of these
 * subcomponents is subject to the terms and conditions of the
 * subcomponent's license, as noted in the LICENSE file.
 */

package org.cloudfoundry.identity.uaa.social;

import static org.junit.Assert.assertTrue;

import org.junit.Assume;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2RestTemplate;
import org.springframework.security.oauth2.client.context.DefaultOAuth2ClientContext;
import org.springframework.security.oauth2.client.context.OAuth2ClientContext;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.AuthenticationScheme;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;

/**
 * @author Dave Syer
 * 
 */
public class OAuth2ClientAuthenticationFilterTests {

	private SocialClientAuthenticationFilter filter = new SocialClientAuthenticationFilter("/login");

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private MockHttpServletResponse response = new MockHttpServletResponse();

	private OAuth2ClientContext context = new DefaultOAuth2ClientContext();

	private void setUpContext(String tokenName) {
		String accessToken = System.getProperty(tokenName);
		Assume.assumeNotNull(accessToken);
		context.setAccessToken(new DefaultOAuth2AccessToken(accessToken));
	}

	@Test
	public void testCloudFoundryAuthentication() throws Exception {
		OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(new AuthorizationCodeResourceDetails(), context);
		setUpContext("cf.token");
		filter.setRestTemplate(restTemplate);
		filter.setUserInfoUrl("https://uaa.cloudfoundry.com/userinfo");
		filter.afterPropertiesSet();
		Authentication authentication = filter.attemptAuthentication(request, response);
		System.err.println(authentication.getDetails());
		assertTrue(authentication.isAuthenticated());
	}

	@Test
	public void testGithubAuthentication() throws Exception {
		OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(new AuthorizationCodeResourceDetails(), context);
		setUpContext("github.token");
		filter.setRestTemplate(restTemplate);
		filter.setUserInfoUrl("https://api.github.com/user");
		filter.afterPropertiesSet();
		Authentication authentication = filter.attemptAuthentication(request, response);
		System.err.println(authentication.getDetails());
		assertTrue(authentication.isAuthenticated());
	}

	@Test
	public void testFacebookAuthentication() throws Exception {
		AuthorizationCodeResourceDetails resource = new AuthorizationCodeResourceDetails();
		resource.setAuthenticationScheme(AuthenticationScheme.query);
		OAuth2RestTemplate restTemplate = new OAuth2RestTemplate(resource, context);
		setUpContext("facebook.token");
		filter.setRestTemplate(restTemplate);
		filter.setUserInfoUrl("https://graph.facebook.com/me");
		filter.afterPropertiesSet();
		Authentication authentication = filter.attemptAuthentication(request, response);
		System.err.println(authentication.getDetails());
		assertTrue(authentication.isAuthenticated());
	}

}
