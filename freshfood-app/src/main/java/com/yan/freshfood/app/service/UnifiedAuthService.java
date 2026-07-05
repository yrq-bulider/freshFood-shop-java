package com.yan.freshfood.app.service;

import com.yan.freshfood.app.vo.UnifiedLoginVO;

public interface UnifiedAuthService {

    UnifiedLoginVO login(String username, String rawPassword);
}