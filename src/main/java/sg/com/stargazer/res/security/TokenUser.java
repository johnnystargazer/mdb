package sg.com.stargazer.res.security;

import java.util.Map;
import java.util.Objects;

import lombok.Value;
import lombok.experimental.Wither;

import com.google.common.collect.Maps;

@Value
public class TokenUser {
    public static final String ACCOUNT_ID = "aid";
    public static final String ACCOUNT_NAME = "an";
    public static final String USER_ROLE = "ur";
    public static final String USER_ID = "uid";
    public static final String ACCOUNT_TYPE = "at";
    public static final String ACCOUNT_PATH = "ap";
    public static final String APPLICATION_ID = "pid";
    public static final String CONTEXT_ID = "ctx";
    public static final String EXTERNAL_WALLET = "extw";
    private Long accountId;
    private AccountType accountType;
    private String accountPath;
    private Long userId;
    private UserRole userRole;
    private String username;
    private Long applicationId;
    private String accountName;
    private Long contextId;
    private Boolean externalWallet;

    public static TokenUser of(Map stringObjectMap) {
        long accountId = Long.valueOf(stringObjectMap.get(ACCOUNT_ID).toString());
        Object accountTypeObj = stringObjectMap.get(ACCOUNT_TYPE);
        AccountType accountType =
            Objects.isNull(accountTypeObj) ? null : AccountType.values()[Integer.valueOf(accountTypeObj.toString())];
        String accountPath = (String) stringObjectMap.getOrDefault(ACCOUNT_PATH, "-1");
        long userId = Long.valueOf(stringObjectMap.getOrDefault(USER_ID, "-1").toString());
        Object roleStr = stringObjectMap.get(USER_ROLE);
        UserRole userRole = Objects.isNull(roleStr) ? null : UserRole.values()[Integer.valueOf(roleStr.toString())];
        Long applicationId = Long.valueOf(stringObjectMap.get(APPLICATION_ID).toString());
        // must have a username for spring to audit
        String username = (String) stringObjectMap.getOrDefault("username", "APPLICATION-" + applicationId);
        String account = (String) stringObjectMap.get(ACCOUNT_NAME);
        long contextId = Long.valueOf(stringObjectMap.getOrDefault(CONTEXT_ID, "-1").toString());
        boolean externalWallet = Boolean.valueOf(stringObjectMap.getOrDefault(EXTERNAL_WALLET, "false").toString());
        return new TokenUser(accountId, accountType, accountPath, userId, userRole, username, applicationId, account,
            contextId, externalWallet);
    }

    public Map toMap() {
        Map result = Maps.newHashMap();
        result.put(TokenUser.ACCOUNT_ID, getAccountId());
        Integer accountTypeOrdinal = Objects.nonNull(accountType) ? accountType.ordinal() : null;
        result.put(TokenUser.ACCOUNT_TYPE, accountTypeOrdinal);
        result.put(TokenUser.ACCOUNT_PATH, getAccountPath()); // should we delete this?
        result.put(TokenUser.USER_ID, getUserId());
        Integer userRoleOrdinal = Objects.nonNull(userRole) ? userRole.ordinal() : null;
        result.put(TokenUser.USER_ROLE, userRoleOrdinal);
        result.put(TokenUser.APPLICATION_ID, getApplicationId());
        result.put(TokenUser.ACCOUNT_NAME, getAccountName());
        if (Objects.nonNull(getContextId())) {
            result.put(TokenUser.CONTEXT_ID, getContextId());
        }
        if (externalWallet) {
            result.put(TokenUser.EXTERNAL_WALLET, true);
        }
        return result;
    }
}
