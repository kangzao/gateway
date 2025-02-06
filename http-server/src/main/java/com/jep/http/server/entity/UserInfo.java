package com.jep.http.server.entity;

import lombok.Builder;
import lombok.Data;

/**
 * @author enping.jep
 * @date 2025/1/28 18:22
 **/
@Data
@Builder
public class UserInfo {
    int id;
    String phoneNumber;
    String name;
    String passwd;
}
