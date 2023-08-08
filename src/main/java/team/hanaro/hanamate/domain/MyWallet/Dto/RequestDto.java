package team.hanaro.hanamate.domain.MyWallet.Dto;

import lombok.*;
import org.springframework.lang.Nullable;

import javax.validation.constraints.NotBlank;

public class RequestDto {

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class MyWallet {
        //TODO: 입력값 Validation Check
        @NotBlank
        private Long walletId;

        @Nullable
        private Integer year;
        @Nullable
        private Integer month;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class AccountBalance {
        @NotBlank
        private Long memberId;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class RequestAmount {
        @NotBlank
        private Long memberId;

        @NotBlank
        private Long walletId;
        @NotBlank
        private Integer amount;
    }

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @ToString
    public static class ConnectAccount {
        @NotBlank
        private Long memberId;

        @NotBlank
        private Long accountId;
        @NotBlank
        private String name;
    }

}
