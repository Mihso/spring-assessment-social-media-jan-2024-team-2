package com.cooksys.socialmedia.controllers;


import com.cooksys.socialmedia.dtos.CredentialsDto;
import com.cooksys.socialmedia.dtos.TweetResponseDto;
import com.cooksys.socialmedia.dtos.UserRequestDto;
import com.cooksys.socialmedia.dtos.UserResponseDto;
import com.cooksys.socialmedia.exceptions.NotFoundException;
import com.cooksys.socialmedia.services.TweetService;
import com.cooksys.socialmedia.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final TweetService tweetService;


    @GetMapping
    public List<UserResponseDto> getAllUsers() {
        return userService.getAllUsers();
    }

    /**
     * Creates a new user. If any required fields are missing or the username provided
     * is already taken, an error should be sent in lieu of a response. If the given
     * credentials match a previously-deleted user, re-activate the deleted user
     * instead of creating a new one.
     * <p>
     * Request:
     * credentials: 'Credentials',
     * profile: 'Profile'
     * <p>
     * Returns:
     * User
     */
    @PostMapping
    public UserResponseDto createUser(@RequestBody UserRequestDto userRequestDto){
        return userService.createUser(userRequestDto);
    }

    /**
     * Retrieves a user with the given username. If no such user exists or is deleted, an error should be sent in lieu of a response.
     *
     * @return 'User' - The user retrieved with the given username.
     */
    @GetMapping("/@{username}")
    public UserResponseDto getUserByUsername(@PathVariable("username") String username) {
        return userService.getUserByUsername(username);
    }

    /**
     * Deletes a user with the given username. If no such user exists or the provided
     * credentials do not match the user, an error should be sent in lieu of a response.
     * If a user is successfully "deleted", the response should contain the user data
     * prior to deletion.
     * <p>
     * IMPORTANT: This action should not actually drop any records from the database!
     * Instead, develop a way to keep track of "deleted" users so that if a user is
     * re-activated, all of their tweets and information are restored.
     * <p>
     * Request:
     * 'Credentials'
     * <p>
     * Response:
     * 'User'
     */
    @DeleteMapping("/@{username}")
    public UserResponseDto deleteUser(@PathVariable("username") String username, @RequestBody CredentialsDto credentials) {
        return userService.deleteUserByUsername(username, credentials);
    }

    /**
     * Unsubscribes the user whose credentials are provided by the request body from
     * the user whose username is given in the URL. If there is no preexisting
     * following relationship between the two users, no such followable user exists
     * (deleted or never created), or the credentials provided do not match an active
     * user in the database, an error should be sent as a response. If successful,
     * no data is sent.
     * <p>
     * Request:
     * 'Credentials'
     */
    @PostMapping("/@{username}/unfollow")
    public void unfollowUser(@PathVariable("username") String username,@RequestBody CredentialsDto credentials) {
        userService.unfollowUser(username, credentials);
    }

    @GetMapping("/@{username}/feed")
    public List<TweetResponseDto> getFeed(@PathVariable("username") String username) {
        return userService.getFeed(username);
    }

    @GetMapping("/@{username}/followers")
    public List<UserResponseDto> getFollowers(@PathVariable("username") String username) {
        return userService.getFollowers(username);
    }

    /**
     * Retrieves the users followed by the user with the given username.
     * Only active users should be included in the response.
     * If no active user with the given username exists, an error should be sent in lieu of a response.
     *
     * @param username The username of the user for whom to retrieve the followed users.
     * @return A list of active users followed by the specified user.
     * The response is in the form of a List of User objects.
     * @throws NotFoundException If no active user is found with the given username.
     */
    @GetMapping("/@{username}/following")
    public List<UserResponseDto> getFollowing(@PathVariable("username") String username) {
        return userService.getFollowing(username);
    }

    /**
     * Retrieves all (non-deleted) tweets authored by the user with the given username.
     * This includes simple tweets, reposts, and replies. The tweets should appear in reverse-chronological order.
     * If no active user with that username exists (deleted or never created), an error should be sent in lieu of a response.
     * <p>
     * Response:
     * ['Tweet']
     */
    @GetMapping("/@{username}/tweets")
    public List<TweetResponseDto> getTweetsByUsername(@PathVariable("username") String username) {
        return userService.getTweetsByUsername(username);
    }

    /**
     * Subscribes the user, whose credentials are provided by the request body,
     * to the user whose username is given in the URL. If there is already a
     * following relationship between the two users, no such followable user exists
     * (deleted or never created), or the credentials provided do not match an
     * active user in the database, an error should be sent as a response.
     * If successful, no data is sent.
     *
     * <p>
     * Request:
     * {@code Credentials}
     * </p>
     */
    @PostMapping("/@{username}/follow")
    public void followUser(@PathVariable("username") String username, @RequestBody CredentialsDto credentialsDto) {
        userService.followUser(username, credentialsDto);
    }
    
    @GetMapping("/@{username}/mentions")
    public List<TweetResponseDto> getTweetsByMentions(@PathVariable("username") String username) {
        return tweetService.getTweetByUserMentions(username);
    }
    @PatchMapping("/@{username}")
    public UserResponseDto updateUser(@PathVariable("username") String username, @RequestBody UserRequestDto userRequestDto) {
        return userService.updateUserProfile(username, userRequestDto);
    }

}