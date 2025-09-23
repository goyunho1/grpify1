//package grpify.grpify.user.controller;
//
//import grpify.grpify.user.domain.User;
//import grpify.grpify.user.dto.SignUpDTO;
//import grpify.grpify.user.dto.UserResponse;
//import grpify.grpify.user.service.UserService;
//import lombok.RequiredArgsConstructor;
//import org.springframework.http.HttpStatus;
//import org.springframework.http.ResponseEntity;
//import org.springframework.web.bind.annotation.*;
//
//@RestController
//@RequestMapping("/api/v1/test-users")
//@RequiredArgsConstructor
//public class UserController {
//
//    private final UserService userService;
//
//    @PostMapping("/signup")
//    public ResponseEntity<UserResponse> signUp(@RequestBody final SignUpDTO signUpDTO) {
//        User newUser = userService.signUp(signUpDTO);
//
//        return new ResponseEntity<>(UserResponse.from(newUser), HttpStatus.CREATED);
//    }
//
//    /**
//     * [임시] 특정 ID로 User 조회 API (테스트용)
//     * GET /api/v1/test-users/{userId}
//     * @param userId 조회할 User의 ID
//     * @return 조회된 User 정보
//     */
//    @GetMapping("/{userId}")
//    public ResponseEntity<UserResponse> getTestUserById(@PathVariable Long userId) {
//        UserResponse response = userService.findById(userId);
//        return new ResponseEntity<>(response, HttpStatus.OK);
//    }
//
//    /**
//     * [임시] 특정 username으로 User 조회 API (테스트용)
//     * GET /api/v1/test-users/username/{username}
//     * @param username 조회할 User의 이름
//     * @return 조회된 User 정보
//     */
//    @GetMapping("/username/{username}")
//    public ResponseEntity<UserResponse> getTestUserByUsername(@PathVariable String username) {
//        UserResponse userResponse = userService.findByName(username);
//        return new ResponseEntity<>(userResponse, HttpStatus.OK);
//    }
//}
