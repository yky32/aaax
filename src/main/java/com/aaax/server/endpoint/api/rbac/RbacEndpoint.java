package com.aaax.server.endpoint.api.rbac;

import com.aaax.core.response.PaginationDto;
import com.aaax.core.response.R;
import com.aaax.core.response.Result;
import com.aaax.server.entity.dto.request.CreateRbacTemplateRequestDto;
import com.aaax.server.entity.dto.response.GetRbacTemplateResponseDto;
import com.aaax.server.usecase.AccessControlUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class RbacEndpoint {

    private final AccessControlUseCase accessControlUseCase;

    @PostMapping("/rbac-templates")
    public Result<Object> create(
            @Valid @RequestBody CreateRbacTemplateRequestDto requestDto
    ) {
        return R.success(accessControlUseCase.createRbacTemplate(requestDto));
    }

    @GetMapping("/rbac-templates")
    public Result<List<GetRbacTemplateResponseDto>> getAll(
            @PageableDefault(page = 1, size = Integer.MAX_VALUE, sort = "createDt", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String startDt,
            @RequestParam(required = false) String endDt
    ) {
        PaginationDto.PaginationDtoBuilder result = accessControlUseCase.getAllRbacTemplates(pageable, startDt, endDt);
        return R.success((List<GetRbacTemplateResponseDto>) result.build().getData(), result.build().getPagination());
    }

    @GetMapping("/rbac-templates/{id}")
    public Result<GetRbacTemplateResponseDto> getOne(
            @PathVariable String id
    ) {
        return R.success(accessControlUseCase.getOneRbacTemplate(id));
    }

}
