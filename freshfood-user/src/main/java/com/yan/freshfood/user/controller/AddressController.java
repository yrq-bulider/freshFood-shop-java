package com.yan.freshfood.user.controller;

import com.yan.freshfood.common.response.R;
import com.yan.freshfood.user.dto.AddressDTO;
import com.yan.freshfood.user.service.AddressService;
import com.yan.freshfood.user.vo.AddressVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public R<List<AddressVO>> list() {
        return R.ok(addressService.listMyAddresses());
    }

    @PostMapping
    public R<AddressVO> create(@Valid @RequestBody AddressDTO dto) {
        return R.ok(addressService.create(dto));
    }

    @PutMapping("/{id}")
    public R<AddressVO> update(@PathVariable Long id, @Valid @RequestBody AddressDTO dto) {
        return R.ok(addressService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable Long id) {
        addressService.delete(id);
        return R.ok();
    }

    @PutMapping("/{id}/default")
    public R<Void> setDefault(@PathVariable Long id) {
        addressService.setDefault(id);
        return R.ok();
    }
}