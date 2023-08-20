package team.hanaro.hanamate.domain.Loan.Controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.Errors;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import team.hanaro.hanamate.domain.Loan.Dto.LoanRequestDto;
import team.hanaro.hanamate.domain.Loan.Service.LoanService;
import team.hanaro.hanamate.domain.User.Helper;
import team.hanaro.hanamate.global.Response;

@Slf4j
@RestController
@RequestMapping("/loan")
@RequiredArgsConstructor

public class LoanController {
    private final LoanService loanService;
    private final Response response;

    //고정이자, 균등상환방식 사용자에게 정보 미리 전달
    @GetMapping("/applyForm")
    public ResponseEntity<?> initLoanInfo(){

        return loanService.initLoanInfo();
    }
    @PostMapping("/apply")
    public ResponseEntity<?> apply(@Validated @RequestBody LoanRequestDto.Apply apply, Errors errors, Authentication authentication) {
//        log.info("대출 컨트롤러 들어옴");
//        log.info("authentication={}", authentication);
        // validation check
        if (errors.hasErrors()) {
            return response.invalidFields(Helper.refineErrors(errors));
        }
        return loanService.apply(apply, authentication);
    }

    @PostMapping("/calculate")
    //TODO: Authentication 추가하기
    public ResponseEntity<?> calculate( @RequestBody LoanRequestDto.Calculate calculate){
        return loanService.calculate(calculate);

    }

    //부모, 아이 - 대출 신청 정보 보기
//    @GetMapping("/applyInfo")
//    public ResponseEntity<?>




//    @GetMapping("/loan/apply")
//    public ResponseEntity<?> apply(@ResponseBody LoanRequestDto.Apply apply, Errors errors){
//
//    }


}