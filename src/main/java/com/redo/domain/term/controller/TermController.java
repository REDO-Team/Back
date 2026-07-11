package com.redo.domain.term.controller;

import com.redo.domain.term.dto.TermResDTO;
import com.redo.domain.term.exception.TermSuccessCode;
import com.redo.domain.term.service.TermService;
import com.redo.global.apiPayload.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermController {

    private final TermService termService;

    @GetMapping
    public ApiResponse<TermResDTO.TermList> getTerms(){
        TermResDTO.TermList result = termService.getTerms();
        return ApiResponse.onSuccess(TermSuccessCode.GET_TERMS_SUCCESS, result);

    }
}