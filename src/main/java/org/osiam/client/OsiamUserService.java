/**
 * The MIT License (MIT)
 *
 * Copyright (C) 2013-2016 tarent solutions GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package org.osiam.client;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import org.glassfish.jersey.client.ClientProperties;
import org.osiam.client.exception.ConnectionInitializationException;
import org.osiam.client.exception.InvalidAttributeException;
import org.osiam.client.oauth.AccessToken;
import org.osiam.client.query.Query;
import org.osiam.client.user.BasicUser;
import org.osiam.resources.scim.SCIMSearchResult;
import org.osiam.resources.scim.UpdateUser;
import org.osiam.resources.scim.User;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;
import java.util.List;

/**
 * The OsiamUserService provides all methods necessary to manipulate the User-entries registered in the given OSIAM
 * installation.
 */
class OsiamUserService extends AbstractOsiamService<User> {

    static final String LEGACY_SCHEMA = "urn:scim:schemas:core:2.0:User";

    OsiamUserService(String endpoint, int connectTimeout, int readTimeout, Version version) {
        super(endpoint, User.class, connectTimeout, readTimeout, version);
    }

    /**
     * See {@link OsiamConnector#getUser(String, AccessToken)}
     */
    User getUser(String id, AccessToken accessToken, String... attributes) {
        return getResource(id, accessToken, attributes);
    }

    /**
     * See {@link OsiamConnector#getCurrentUserBasic(AccessToken)}
     *
     * @deprecated The BasicUser class has been deprecated. Use {@link #getMe(AccessToken)} with OSIAM 3.x. This method
     * is going to go away with version 1.12 or 2.0.
     */
    @Deprecated
    BasicUser getCurrentUserBasic(AccessToken accessToken) {
        checkAccessTokenIsNotNull(accessToken);

        StatusType status;
        String content;
        try {
            Response response = targetEndpoint.path("me").request(MediaType.APPLICATION_JSON)
                    .header("Authorization", BEARER + accessToken.getToken())
                    .property(ClientProperties.CONNECT_TIMEOUT, getConnectTimeout())
                    .property(ClientProperties.READ_TIMEOUT, getReadTimeout())
                    .get();

            status = response.getStatusInfo();
            content = response.readEntity(String.class);
        } catch (ProcessingException e) {
            throw new ConnectionInitializationException(CONNECTION_SETUP_ERROR_STRING, e);
        }

        checkAndHandleResponse(content, status, accessToken);

        return mapToType(content, BasicUser.class);
    }

    /**
     * See {@link OsiamConnector#getCurrentUser(AccessToken)}
     *
     * @deprecated Use {@link #getMe(AccessToken, String...)} with OSIAM 3.x. This method
     * is going to go away with version 1.12 or 2.0.
     */
    @Deprecated
    User getCurrentUser(AccessToken accessToken) {
        BasicUser basicUser = getCurrentUserBasic(accessToken);
        return getResource(basicUser.getId(), accessToken);
    }

    /**
     * See {@link OsiamConnector#getMe(AccessToken, String...)}
     */
    User getMe(AccessToken accessToken, String... attributes) {
        return getVersion() == Version.OSIAM_3
                ? mapToType(getMeResource(accessToken, attributes), User.class)
                : getUser(getCurrentUserBasic(accessToken).getId(), accessToken);
    }

    /**
     * See {@link OsiamConnector#getAllUsers(AccessToken, String...)}
     */
    List<User> getAllUsers(AccessToken accessToken, String... attributes) {
        return super.getAllResources(accessToken, attributes);
    }

    /**
     * See {@link OsiamConnector#searchUsers(Query, AccessToken)}
     */
    SCIMSearchResult<User> searchUsers(Query query, AccessToken accessToken) {
        return searchResources(query, accessToken);
    }

    /**
     * See {@link OsiamConnector#deleteUser(String, AccessToken)}
     */
    void deleteUser(String id, AccessToken accessToken) {
        deleteResource(id, accessToken);
    }

    /**
     * See {@link OsiamConnector#createUser(User, AccessToken)}
     */
    User createUser(User user, AccessToken accessToken) {
        return createResource(user, accessToken);
    }

    /**
     * See {@link OsiamConnector#updateUser(String, UpdateUser, AccessToken)}
     *
     * @deprecated Updating with PATCH has been removed in OSIAM 3.0. This method is going to go away with version 1.12 or 2.0.
     */
    @Deprecated
    User updateUser(String id, UpdateUser updateUser, AccessToken accessToken) {
        if (updateUser == null) {
            throw new IllegalArgumentException("The given updateUser can't be null.");
        }
        return updateResource(id, updateUser.getScimConformUpdateUser(), accessToken);
    }

    /**
     * See {@link OsiamConnector#replaceUser(String, User, AccessToken)}
     */
    User replaceUser(String id, User user, AccessToken accessToken) {
        if (user == null) {
            throw new InvalidAttributeException("The given User can't be null.");
        }
        if (Strings.isNullOrEmpty(id)) {
            throw new InvalidAttributeException("The given User ID can't be null or empty.");
        }
        return replaceResource(id, user, accessToken);
    }

    @Override
    protected String getSchema() {
        return User.SCHEMA;
    }

    @Override
    protected String getLegacySchema() {
        return LEGACY_SCHEMA;
    }

    private String getMeResource(AccessToken accessToken, String... attributes) {
        checkAccessTokenIsNotNull(accessToken);
        WebTarget target;
        if (attributes == null || attributes.length == 0) {
            target = targetEndpoint;
        } else {
            target = targetEndpoint.queryParam("attributes", Joiner.on(",").join(attributes));
        }

        StatusType status;
        String content;
        try {
            Response response = target.path("Me").request(MediaType.APPLICATION_JSON)
                    .header("Authorization", BEARER + accessToken.getToken())
                    .property(ClientProperties.CONNECT_TIMEOUT, getConnectTimeout())
                    .property(ClientProperties.READ_TIMEOUT, getReadTimeout())
                    .get();

            status = response.getStatusInfo();
            content = response.readEntity(String.class);
        } catch (ProcessingException e) {
            throw new ConnectionInitializationException(CONNECTION_SETUP_ERROR_STRING, e);
        }

        checkAndHandleResponse(content, status, accessToken);
        return content;
    }
}
