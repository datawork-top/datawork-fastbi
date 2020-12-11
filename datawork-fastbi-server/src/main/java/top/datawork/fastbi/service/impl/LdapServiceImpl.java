/*
 * <<
 *  EDP
 *  ==
 *  Copyright (C) 2016 - 2019 EDP
 *  ==
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *        http://www.apache.org/licenses/LICENSE-2.0
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *  >>
 *
 */

package top.datawork.fastbi.service.impl;

import top.datawork.core.exception.ServerException;
import top.datawork.fastbi.core.enums.UserOrgRoleEnum;
import top.datawork.fastbi.dao.OrganizationMapper;
import top.datawork.fastbi.dao.RelUserOrganizationMapper;
import top.datawork.fastbi.dao.UserMapper;
import top.datawork.fastbi.model.LdapPerson;
import top.datawork.fastbi.model.Organization;
import top.datawork.fastbi.model.RelUserOrganization;
import top.datawork.fastbi.model.User;
import top.datawork.fastbi.service.LdapService;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import top.datawork.core.consts.Consts;
import top.datawork.fastbi.core.common.Constants;

import javax.naming.directory.DirContext;
import java.util.List;

import static org.springframework.ldap.query.LdapQueryBuilder.query;


@Slf4j
@Service("ldapService")
public class LdapServiceImpl implements LdapService {

    @Autowired
    private LdapTemplate ldapTemplate;

    @Value("${spring.ldap.domainName}")
    private String ldapDomainName;

    @Value("${spring.ldap.urls:''}")
    private String ldapUrls;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private OrganizationMapper organizationMapper;

    @Autowired
    private RelUserOrganizationMapper relUserOrganizationMapper;

    public boolean existLdapServer() {
        return !StringUtils.isEmpty(ldapUrls);
    }

    /**
     * 查找 ldap 用户
     *
     * @param username
     * @param password
     * @return
     * @throws Exception
     */
    @Override
    public LdapPerson findByUsername(String username, String password) {
		LdapPerson ldapPerson = null;

		if (StringUtils.endsWithIgnoreCase(username, ldapDomainName)) {
			username = username.replaceAll("(?i)" + ldapDomainName, Consts.EMPTY);
		}
		String userDn = username + ldapDomainName;

		DirContext ctx = null;
		try {
			ctx = ldapTemplate.getContextSource().getContext(userDn, password);

			List<LdapPerson> search = ldapTemplate.search(
					query().where("objectclass").is("person").and("sAMAccountName").is(username),
					(AttributesMapper<LdapPerson>) attributes -> {
						LdapPerson person = new LdapPerson();
						person.setName(attributes.get("cn").get().toString());
						person.setSAMAccountName(attributes.get("sAMAccountName").get().toString());
						person.setEmail(attributes.get("mail").get().toString());
						return person;
					});

			if (!CollectionUtils.isEmpty(search)) {
				ldapPerson = search.get(0);
			}
		} catch (Exception e) {
			log.error(e.toString(), e);
		} finally {
			if (null != ctx) {
				LdapUtils.closeContext(ctx);
			}
		}

		return ldapPerson;
    }

    @Override
    @Transactional
    public User registPerson(LdapPerson ldapPerson) throws ServerException {
        User user = new User(ldapPerson);
        user.setActive(true);
        user.setPassword(Constants.LDAP_USER_PASSWORD);

        if (userMapper.insert(user) <= 0) {
            log.error("Ldap regist fail, email({})", user.getEmail());
            throw new ServerException("Ldap regist fail");
        }
        
        String orgName = user.getUsername() + "'s Organization";
        Organization organization = new Organization(orgName, null, user.getId());
        if (organizationMapper.insert(organization) > 0) {
            RelUserOrganization relUserOrganization = new RelUserOrganization(organization.getId(), user.getId(), UserOrgRoleEnum.OWNER.getRole());
            relUserOrganization.createdBy(user.getId());
            relUserOrganizationMapper.insert(relUserOrganization);
        }

        return user;
    }
}
