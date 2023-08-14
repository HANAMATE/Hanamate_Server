package team.hanaro.hanamate.domain.Allowance;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import team.hanaro.hanamate.domain.Allowance.Dto.RequestDto;
import team.hanaro.hanamate.domain.Allowance.Dto.ResponseDto;
import team.hanaro.hanamate.domain.MyWallet.Repository.TransactionRepository;
import team.hanaro.hanamate.domain.MyWallet.Repository.WalletRepository;
import team.hanaro.hanamate.domain.User.Repository.UsersRepository;
import team.hanaro.hanamate.entities.*;
import team.hanaro.hanamate.global.Response;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Optional;

@RequiredArgsConstructor
@Service
public class AllowanceService {

    private final RequestsRepository requestsRepository;
    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;
    private final AllowancesRepository allowancesRepository;
    private final UsersRepository usersRepository;
    private final Response response;

    /* 1. 아이 : 용돈 조르기(대기중) 요청 조회*/
    public ResponseEntity<?> getMyAllowancePendingRequestList(RequestDto.User user) {
        System.out.println("userIdx: " + user.getUserIdx());
        Optional<List<Requests>> myRequests = requestsRepository.findAllByRequesterIdxAndAskAllowanceIsNull(user.getUserIdx());

        if (myRequests.isEmpty() || myRequests.get().isEmpty()) {
            return response.fail("대기 상태의 용돈 조르기 요청이 없습니다.", HttpStatus.BAD_REQUEST);
        }

        List<Requests> requestList = myRequests.get();
        System.out.println(requestList.toString());
        List<ResponseDto.Request> responseRequestList = new ArrayList<>();
        for (Requests request : requestList) {
            ResponseDto.Request responseRequest = new ResponseDto.Request(request);
            responseRequestList.add(responseRequest);
        }
        return response.success(responseRequestList, "용돈 조르기(대기중) 요청 리스트 조회에 성공했습니다.", HttpStatus.OK);
    }

    /* 2. 아이 : 용돈 조르기(승인/거절) 요청 조회*/
    public ResponseEntity<?> getMyAllowanceApprovedRequestList(RequestDto.User user) {
        Optional<List<Requests>> myRequests = requestsRepository.findAllByRequesterIdxAndAskAllowanceIsNotNull(user.getUserIdx());
        if (myRequests.isEmpty()) {
            return response.fail("승인/거절된 용돈 조르기 요청이 없습니다.", HttpStatus.BAD_REQUEST);
        }

        List<Requests> requestList = myRequests.get();
        List<ResponseDto.Request> responseRequestList = new ArrayList<>();
        for (Requests request : requestList) {
            ResponseDto.Request responseRequest = new ResponseDto.Request(request);
            responseRequestList.add(responseRequest);
        }
        return response.success(responseRequestList, "용돈 조르기(승인/거절) 요청 리스트 조회에 성공했습니다.", HttpStatus.OK);
    }

    /* 3. 아이 : 용돈 조르기 생성 */
    public ResponseEntity<?> makeAllowanceRequest(RequestDto.Request request) {
        Calendar cal = Calendar.getInstance();
        Timestamp requestDate = new Timestamp(cal.getTimeInMillis());
        cal.add(Calendar.DATE, 7);
        Timestamp expiredDate = new Timestamp(cal.getTimeInMillis());

        Optional<User> child = usersRepository.findByIdx(request.getChildIdx());
        Optional<User> parent = usersRepository.findByIdx(request.getParentIdx());

        if (child.isEmpty()) {
            return response.fail("childId가 잘못되었습니다.", HttpStatus.BAD_REQUEST);
        }

        if (parent.isEmpty()) {
            return response.fail("parentId가 잘못되었습니다.", HttpStatus.BAD_REQUEST);
        }

        Requests requests = Requests.builder()
                .targetIdx(request.getParentIdx()) //TODO: 부모 아이디로 설정 [코드 작성 08.11 / 안식]
                .requesterIdx(request.getChildIdx())
                .allowanceAmount(request.getAllowanceAmount())
                .requestDate(requestDate)
                .expirationDate(expiredDate)
                .requestDescription(request.getRequestDescription())
                .build();
        requestsRepository.save(requests);
        requestsRepository.flush();

        return response.success("용돈 조르기에 성공했습니다.");
    }

    /* 4. 부모 : 용돈 조르기(대기중) 요청 조회 */
    public ResponseEntity<?> getMyChildAllowancePendingRequestList(RequestDto.User user) {
        Optional<List<Requests>> myRequests = requestsRepository.findAllByTargetIdxAndAskAllowanceIsNull(user.getUserIdx());

        if (myRequests.isEmpty()) {
            return response.fail("대기 상태의 용돈 조르기 요청이 없습니다.", HttpStatus.BAD_REQUEST);
        }

        List<Requests> requestList = myRequests.get();
        List<ResponseDto.Request> responseRequestList = new ArrayList<>();
        for (Requests request : requestList) {
            ResponseDto.Request responseRequest = new ResponseDto.Request(request);
            responseRequestList.add(responseRequest);
        }
        return response.success(responseRequestList, "용돈 조르기(대기) 리스트 조회에 성공했습니다.", HttpStatus.OK);
    }

    /* 5. 부모 : 용돈 조르기(승인,거절) 요청 조회 */
    public ResponseEntity<?> getMyChildAllowanceApprovedRequestList(RequestDto.User user) {
        Optional<List<Requests>> myRequests = requestsRepository.findAllByTargetIdxAndAskAllowanceIsNotNull(user.getUserIdx());

        if (myRequests.isEmpty()) {
            return response.fail("대기 상태의 용돈 조르기 요청이 없습니다.", HttpStatus.BAD_REQUEST);
        }

        List<Requests> requestList = myRequests.get();
        List<ResponseDto.Request> responseRequestList = new ArrayList<>();
        for (Requests request : requestList) {
            ResponseDto.Request responseRequest = new ResponseDto.Request(request);
            responseRequestList.add(responseRequest);
        }
        return response.success(responseRequestList, "용돈 조르기(성공,실패) 리스트 조회에 성공했습니다.", HttpStatus.OK);
    }

    /* 6. 부모 : 용돈 조르기 상태 변경(대기중 -> 승인/거절) */
    @Transactional
    public ResponseEntity<?> updateRequestStatus(RequestDto.Approve approve) {
        Optional<Requests> request = requestsRepository.findByRequestId(approve.getRequestId());

        if (request.isEmpty()) {
            return response.fail("유효하지 않은 requestId 입니다.", HttpStatus.BAD_REQUEST);
        }

        if (request.get().getAskAllowance() != null) {
            return response.fail("대기중인 상태의 용돈 조르기 요청만 상태 update가 가능합니다.", HttpStatus.BAD_REQUEST);
        }

        Optional<User> parent = usersRepository.findById(request.get().getTargetIdx());
        MyWallet parentWallet = parent.get().getMyWallet();

        if (parent.isEmpty()) {
            return response.fail("해당 용돈 조르기 요청의 부모Id가 올바르지 않습니다.", HttpStatus.BAD_REQUEST);
        }


        // 거절
        if (!approve.getAskAllowance()) {
            request.get().setAskAllowance(approve.getAskAllowance());
            request.get().setChangedDate(new Timestamp(Calendar.getInstance().getTimeInMillis()));
            requestsRepository.save(request.get());
            return response.success("용돈 조르기 요청을 거절했습니다.");
        }
        // 승인
        else {
            // 1. 잔액 부족
            if (parentWallet.getBalance() < approve.getRequestId()) {
                return response.fail("잔액이 부족합니다.", HttpStatus.BAD_REQUEST);
            }
            // 2. 성공
            Optional<User> child = usersRepository.findById(request.get().getRequestId());
            MyWallet childWallet = child.get().getMyWallet();

            if (child.isEmpty()) {
                response.fail("용돈 조르기 요청의 아이Id가 올바르지 않습니다.", HttpStatus.BAD_REQUEST);
            }

            // 2-1. Transaction 작성
            makeTransaction(parentWallet, childWallet, request.get().getAllowanceAmount());
            // 2-2. 아이 지갑 +
            childWallet.setBalance(childWallet.getBalance() + request.get().getAllowanceAmount());
            walletRepository.save(childWallet);
            // 2-3. 부모 지갑 -
            parentWallet.setBalance(parentWallet.getBalance() - request.get().getAllowanceAmount());
            walletRepository.save(parentWallet);
            // 2-4. request 변경
            request.get().setAskAllowance(approve.getAskAllowance());
            request.get().setChangedDate(new Timestamp(Calendar.getInstance().getTimeInMillis()));
            requestsRepository.save(request.get());
            walletRepository.flush();
            requestsRepository.flush();
            return response.success("용돈 조르기 요청을 승인했습니다.");
        }
    }

    /* 7. 부모 : 용돈 보내기 */
    @Transactional
    public ResponseEntity<?> sendAllowance(RequestDto.Request request) {
        Optional<User> child = usersRepository.findById(request.getChildIdx());
        Optional<User> parent = usersRepository.findById(request.getParentIdx());

        if (child.isEmpty()) {
            response.fail("아이Id가 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }
        if (parent.isEmpty()) {
            response.fail("부모Id가 존재하지 않습니다.", HttpStatus.BAD_REQUEST);
        }

        MyWallet childWallet = child.get().getMyWallet();
        MyWallet parentWallet = parent.get().getMyWallet();

        // 1. Transaction 작성
        makeTransaction(parentWallet, childWallet, request.getAllowanceAmount());
        // 2. 아이 지갑 +
        childWallet.setBalance(childWallet.getBalance() + request.getAllowanceAmount());
        walletRepository.save(childWallet);
        // 3. 부모 지갑 -
        parentWallet.setBalance(parentWallet.getBalance() - request.getAllowanceAmount());
        walletRepository.save(parentWallet);

        return response.success("용돈 이체에 성공했습니다.");
    }

    private void makeTransaction(MyWallet parent, MyWallet child, Integer amount) {
        // 부모 -> 아이 거래내역
        Transactions parentToChildTransaction = Transactions.builder()
                .id(parent.getId())
                .counterId(child.getId())
                .transactionDate(new Timestamp(Calendar.getInstance().getTimeInMillis()))
                .transactionType("용돈 이체")
                .amount(amount)
                .balance((int) (parent.getBalance() - amount))
                .build();
        // 아이 -> 부모 거래내역
        Transactions childToParentTransaction = Transactions.builder()
                .id(child.getId())
                .counterId(parent.getId())
                .transactionDate(new Timestamp(Calendar.getInstance().getTimeInMillis()))
                .transactionType("용돈 입금")
                .amount(amount)
                .balance((int) (child.getBalance() + amount))
                .build();

        transactionRepository.save(parentToChildTransaction);
        transactionRepository.save(childToParentTransaction);
        transactionRepository.flush();
    }

    /* 8. 부모 : 정기 용돈 조회 */
    public ResponseEntity<?> getPeriodicAllowance(RequestDto.User user) {
        Optional<List<Allowances>> allowances = allowancesRepository.findAllByParentIdxAndValidIsTrue(user.getUserIdx());

        if (allowances.isPresent() && !allowances.get().isEmpty()) {
            List<Allowances> allowancesList = allowances.get();
            List<ResponseDto.Allowance> responseAllowanceList = new ArrayList<>();
            for (Allowances allowance : allowancesList) {
                ResponseDto.Allowance responseAllowance = new ResponseDto.Allowance(allowance);
                responseAllowanceList.add(responseAllowance);
            }
            return response.success(responseAllowanceList, "정기 용돈 조회에 성공했습니다.", HttpStatus.OK);
        } else {
            return response.fail("정기 용돈 리스트가 없습니다.", HttpStatus.BAD_REQUEST);
        }

    }

    /* 9. 부모 : 정기 용돈 생성 */
    public ResponseEntity<?> makePeriodicAllowance(RequestDto.Periodic periodic) {
        /* 아이-부모 정기 용돈은 최대 한개까지 */
        Optional<Allowances> myallowance = allowancesRepository.findByChildrenIdxAndParentIdxAndValidIsTrue(periodic.getChildIdx(), periodic.getParentIdx());
        if (myallowance.isPresent()) {
            return response.fail("아이-부모 사이에 정기 용돈이 존재합니다.", HttpStatus.BAD_REQUEST);
        }

        if (!isValidPeriodicCondition(periodic.getEveryday(), periodic.getDayOfWeek(), periodic.getTransferDate())) {
            return response.fail("정기적으로 용돈을 줄 날짜를 잘못 입력했습니다.", HttpStatus.BAD_REQUEST);
        }

        Allowances allowance = Allowances.builder()
                .childrenIdx(periodic.getChildIdx())
                .parentIdx(periodic.getParentIdx())
                .allowanceAmount(periodic.getAllowanceAmount())
                .transferDate(periodic.getTransferDate())
                .dayOfWeek(periodic.getDayOfWeek())
                .everyday(periodic.getEveryday())
                .valid(true)
                .build();

        allowancesRepository.save(allowance);

        return response.success("정기 용돈을 생성했습니다.");

    }

    /* 10. 부모 : 정기 용돈 업데이트 */
    @Transactional
    public ResponseEntity<?> updatePeriodicAllowance(RequestDto.UpdatePeriodic periodic) {
        Optional<Allowances> allowances = allowancesRepository.findByAllowanceIdAndValidIsTrue(periodic.getAllowanceId());

        if (allowances.isEmpty()) {
            return response.fail("해당 Id의 정기 용돈이 없습니다.", HttpStatus.BAD_REQUEST);
        }

        if (!isValidPeriodicCondition(periodic.getEveryday(), periodic.getDayOfWeek(), periodic.getTransferDate())) {
            return response.fail("정기적으로 용돈을 줄 날짜를 잘못 입력했습니다.", HttpStatus.BAD_REQUEST);
        }

        allowances.get().setAllowanceAmount(periodic.getAllowanceAmount());
        allowances.get().setTransferDate(periodic.getTransferDate());
        allowances.get().setDayOfWeek(periodic.getDayOfWeek());
        allowances.get().setEveryday(periodic.getEveryday());

        allowancesRepository.save(allowances.get());

        return response.success("정기 용돈 정보를 Update 했습니다.");
    }

    /* 11. 부모 : 정기 용돈 삭제 */
    @Transactional
    public ResponseEntity<?> deletePeriodicAllowance(RequestDto.Allowance periodic) {
        Optional<Allowances> allowance = allowancesRepository.findByAllowanceIdAndValidIsTrue(periodic.getAllowanceId());
        if (allowance.isEmpty()) {
            return response.fail("해당 Id의 정기 용돈이 없습니다.", HttpStatus.BAD_REQUEST);
        }
        allowance.get().setValid(false);
        allowancesRepository.save(allowance.get());
        return response.success("정기 용돈을 삭제했습니다.");
    }

    private boolean isValidPeriodicCondition(Boolean everyday, Integer dayOfWeek, Integer transferDate) {
        // 모두 다 0
        boolean isPeriodicNoneExists = everyday.equals(false) && dayOfWeek.equals(0) && transferDate.equals(0);
        // 둘 이상 값이 있을 경우
        boolean isPeriodicConditionMorethanTwoExists = (!everyday.equals(false) && !dayOfWeek.equals(0))
                || (!everyday.equals(false) && !transferDate.equals(0))
                || (!dayOfWeek.equals(0) && !transferDate.equals(0));

        if (isPeriodicConditionMorethanTwoExists || isPeriodicNoneExists) {
            return false;
        }
        return true;
    }
}
