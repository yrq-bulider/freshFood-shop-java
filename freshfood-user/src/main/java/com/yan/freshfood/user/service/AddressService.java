package com.yan.freshfood.user.service;

import com.yan.freshfood.user.dto.AddressDTO;
import com.yan.freshfood.user.vo.AddressVO;

import java.util.List;

public interface AddressService {
    List<AddressVO> listMyAddresses();
    AddressVO create(AddressDTO dto);
    AddressVO update(Long id, AddressDTO dto);
    void delete(Long id);
    void setDefault(Long id);
}