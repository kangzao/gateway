package com.jep.user.server.entity;

import lombok.Builder;
import lombok.Data;

/**
 * @author enping.jep
 * @date 2025/2/23 15:19
 **/
@Data
@Builder
public class UserInfo {
    int id;
    String phoneNumber;
    String name;
    String passwd;
}
