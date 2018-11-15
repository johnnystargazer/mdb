package sg.com.stargazer.res.security;

/**
 * Represents what type an Account
 * is in our System.
 */
public enum AccountType {
    UNKNOWN {
        @Override
        public boolean isValidChildOf(AccountType type) {
            return false;
        }

        @Override
        public boolean canCreateType(AccountType type) {
            return false;
        }
    },
    /**
     * The main account and root of all other accounts
     * Top of the tree
     */
    SYSTEM {
        @Override
        public boolean isValidChildOf(AccountType type) {
            return false;
        }

        @Override
        public boolean canCreateType(AccountType type) {
            return true;
        }
    },
    /**
     * A Head office is always under System
     * and can have multiple Companies underit
     */
    HEAD_OFFICE {
        @Override
        public boolean isValidChildOf(AccountType type) {
            if (type.equals(AccountType.SYSTEM)) {
                return true;
            }
            return false;
        }

        @Override
        public boolean canCreateType(AccountType type) {
            if (type == AccountType.SUB_COMPANY || type == AccountType.MEMBER) {
                return true;
            }
            return false;
        }
    },
    /**
     * A company is always under a Head Office
     */
    COMPANY {
        @Override
        public boolean isValidChildOf(AccountType type) {
            if (type.equals(AccountType.HEAD_OFFICE)) {
                return true;
            }
            return false;
        }

        @Override
        public boolean canCreateType(AccountType type) {
            if (type == AccountType.SUB_COMPANY || type == AccountType.MEMBER) {
                return true;
            }
            return false;
        }
    },
    /**
     * A Sub company can be under a Company or
     * under another Sub Company up to the amount
     * of levels the system allows
     */
    SUB_COMPANY {
        @Override
        public boolean isValidChildOf(AccountType type) {
            if (type.equals(AccountType.COMPANY) || type.equals(AccountType.SUB_COMPANY)) {
                return true;
            }
            return false;
        }

        @Override
        public boolean canCreateType(AccountType type) {
            if (type == AccountType.SUB_COMPANY || type == AccountType.MEMBER) {
                return true;
            }
            return false;
        }
    },
    /**
     * A Member can be under a Company or
     * a Sub Company
     */
    MEMBER {
        @Override
        public boolean isValidChildOf(AccountType type) {
            if (type.equals(AccountType.COMPANY) || type.equals(AccountType.SUB_COMPANY)) {
                return true;
            }
            return false;
        }

        @Override
        public boolean canCreateType(AccountType type) {
            return false;
        }
    };
    public abstract boolean isValidChildOf(AccountType type);

    public abstract boolean canCreateType(AccountType type);
}
