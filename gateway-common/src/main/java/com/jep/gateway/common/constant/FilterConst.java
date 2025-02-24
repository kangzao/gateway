package com.jep.gateway.common.constant;

/**
 * @author enping.jep
 * @date 2025/1/27 22:07
 **/
public interface FilterConst {

    /**
     * Prometheus 入口过滤器
     */
    String MONITOR_FILTER_ID = "monitor_filter";
    String MONITOR_FILTER_NAME = "monitor_filter";
    int MONITOR_FILTER_ORDER = -1;

    /**
     * Prometheus 出口过滤器
     */
    String MONITOR_END_FILTER_ID = "monitor_end_filter";
    String MONITOR_END_FILTER_NAME = "monitor_end_filter";
    int MONITOR_END_FILTER_ORDER = Integer.MAX_VALUE - 1;

    /**
     * 负载均衡过滤器
     */
    String LOAD_BALANCE_FILTER_ID = "load_balance_filter";
    String LOAD_BALANCE_FILTER_NAME = "load_balance_filter";
    int LOAD_BALANCE_FILTER_ORDER = 100;
    String LOAD_BALANCE_KEY = "load_balance";
    String LOAD_BALANCE_STRATEGY_RANDOM = "Random";
    String LOAD_BALANCE_STRATEGY_ROUND_ROBIN = "RoundRobin";
    String LOAD_BALANCE_STRATEGY_WEIGHT_RANDOM = "WeightRandom";

    /**
     * 路由过滤器
     */
    String ROUTER_FILTER_ID = "router_filter";
    String ROUTER_FILTER_NAME = "router_filter";
    int ROUTER_FILTER_ORDER = Integer.MAX_VALUE;

    /**
     * 限流过滤器
     */
    String FLOW_CTL_FILTER_ID = "flow_ctl_filter";
    String FLOW_CTL_FILTER_NAME = "flow_ctl_filter";
    int FLOW_CTL_FILTER_ORDER = 50;
    String FLOW_CTL_TYPE_PATH = "path";
    String FLOW_CTL_TYPE_SERVICE = "service";
    String FLOW_CTL_LIMIT_DURATION = "duration";    //限流时间单位——秒
    String FLOW_CTL_LIMIT_PERMITS = "permits";      //限流请求次数——次
    String FLOW_CTL_MODE_DISTRIBUTED = "distributed"; //分布式场景
    String FLOW_CTL_MODE_SINGLETON = "singleton";     //单例场景

    /**
     * 认证鉴权过滤器
     */
    String AUTH_FILTER_ID = "auth_filter";
    String AUTH_FILTER_NAME = "auth_filter";
    String AUTH_FILTER_KEY = "auth_path";
    int AUTH_FILTER_ORDER = 1;
    String TOKEN_USERID_KEY = "userId";
    String TOKEN_SECRET = "123456"; //一般不会写在代码里 单独存放
    String COOKIE_KEY = "gateway-jwt";


    /**
     * 灰度发布过滤器
     */
    String GRAY_FILTER_ID = "gray_filter";
    String GRAY_FILTER_NAME = "gray_filter";
    int GRAY_FILTER_ORDER = 0;
    String GRAY_FILTER_KEY = "gray_release";

    String PART_GRAY = "part_gray";

    /**
     * mock过滤器
     */
    String MOCK_FILTER_ID = "mock_filter";
    String MOCK_FILTER_NAME = "mock_filter";
    int MOCK_FILTER_ORDER = 0;


    /**
     * URL加密对称密钥保存过滤器
     */
    String SYMMETRIC_KEY_FILTER_ID = "symmetric_key_filter";
    String SYMMETRIC_KEY_FILTER_NAME = "symmetric_key_filter";
    int SYMMETRIC_KEY_FILTER_ORDER = 2;

    /**
     * AES公私钥的有效期
     */
    int SYMMETRIC_KEY_EXPIRE_TIME = 3600;
    int RSA_PRIVATE_KEY_EXPIRE_TIME = 24 * 3600;

    /**
     * 前端请求头中携带的AES公钥标识
     */
    String SYMMETRIC_KEY_PUBLIC_KEY = "X-Encrypted-Symmetric-key";

    /**
     * 后端存储 AES对称密钥/RSA私钥的 命名前缀（zset结构）
     */
    String SECURITY_KEY_PREFIX = "security:key";
    String SYMMETRIC_KEY_PREFIX = "symmetric:key";

    /**
     * 后端存储RSA私钥到Redis中的标识，因为是不同的服务（网关服务、提供公钥的服务，不同服务间无法共享数据，需要通过 Redis 实现共享）
     */
    String RSA_PRIVATE_KEY_PREFIX = "rsa:key";

    /**
     * URL加密验证过滤器
     */
    String CRYPTO_FILTER_ID = "crypto_filter";
    String CRYPTO_FILTER_NAME = "crypto_filter";
    int CRYPTO_FILTER_ORDER = 3;
    String CRYPTO_PUBLIC_KEY = "X-Encrypted-Symmetric-key";

    /**
     * 加密验证白名单，比如登录不需要加密验证 url
     */
    String WHITE_LIST_KEY = "white_list";
}
