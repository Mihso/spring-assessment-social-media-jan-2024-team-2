package com.cooksys.socialmedia.services.impl;

import com.cooksys.socialmedia.dtos.*;
import com.cooksys.socialmedia.entities.Tweet;
import com.cooksys.socialmedia.entities.User;
import com.cooksys.socialmedia.exceptions.BadRequestException;
import com.cooksys.socialmedia.exceptions.NotAuthorizedException;
import com.cooksys.socialmedia.exceptions.NotFoundException;
import com.cooksys.socialmedia.mappers.CredentialsMapper;
import com.cooksys.socialmedia.mappers.TweetMapper;
import com.cooksys.socialmedia.mappers.UserMapper;
import com.cooksys.socialmedia.repositories.UserRepository;
import com.cooksys.socialmedia.services.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final TweetMapper tweetMapper;
    private final CredentialsMapper credentialsMapper;

    private User getUserHelper(String username) {
        Optional<User> userToCheckFor = userRepository.findByCredentials_Username(username);

        if (userToCheckFor.isEmpty() || userToCheckFor.get().isDeleted()) {
            throw new NotFoundException("No user found with username: '" + username + "'");
        }
        return userToCheckFor.get();
    }

    @Override
    public List<UserResponseDto> getAllUsers() {
    	List<UserResponseDto> lister =  userMapper.entitiesToDtos(userRepository.findAll());
    	
    	for(UserResponseDto u: lister) {
    		if(userMapper.responseDtoToEntity(u).isDeleted()) {
    			lister.remove(u);
    		}
    	}
    	
    	return lister;
    	
    }

    @Override
    public UserResponseDto createUser(UserRequestDto userRequestDto) {
        User u = new User();
        CredentialsDto credentials = userRequestDto.getCredentials();
        ProfileDto profile = userRequestDto.getProfile();
        
        if (credentials == null || profile == null || profile.getEmail() == null || credentials.getPassword() == null || credentials.getUsername() == null) {
            throw new BadRequestException("A required parameter is missing");
        }
        
        for (User use : userRepository.findAll()) {
            if (use.getCredentials().getUsername().equals(credentials.getUsername())) {
                if (use.isDeleted()) {
                    use.setDeleted(false);
                    userRepository.flush();
                    return userMapper.entityToDto(use);
                } else {
                    throw new BadRequestException("This username is already in use.");
                }
            }
        }

        u.setProfile(userMapper.requestDtoToEntity(userRequestDto).getProfile());
        u.setCredentials(userMapper.requestDtoToEntity(userRequestDto).getCredentials());

        return userMapper.entityToDto(userRepository.saveAndFlush(u));
    }

    @Override
    public UserResponseDto getUserByUsername(String username) {
        return userMapper.entityToDto(getUserHelper(username));
    }

    @Override
    public UserResponseDto deleteUserByUsername(String username, CredentialsDto credentials) {
        User current = new User();
        for (User u : userRepository.findAll()) {
            if (userMapper.entityToDto(u).getUsername().equals(username) && !u.isDeleted()) {
                current = u;
            }
        }

        if (userMapper.entityToDto(current).getUsername() == null) {
            throw new NotFoundException("No user with the given username.");
        }

        if (credentialsMapper.entityToDto(current.getCredentials()).equals(credentials)) {
            current.setDeleted(true);
        } else {
            throw new NotAuthorizedException("Credentials wrong.");
        }

        userRepository.flush();

        return userMapper.entityToDto(current);
    }

    @Override
    public void unfollowUser(String username, CredentialsDto credentials) {
    	
    	if(username == null) {
    		throw new BadRequestException("No username given.");
    	}
    	
        List<User> userList = userRepository.findAll();
        User current = new User();
        User toUnfollow = new User();
        for(User u: userList) {
        	if(credentialsMapper.entityToDto(u.getCredentials()).equals(credentials) && u.isDeleted() == false) {
        		current = u;
        	}
        	if(u.getCredentials().getUsername().equals(username) && !u.isDeleted()) {
        		toUnfollow = u;
        	}
        }
        
        if(current.getCredentials() == null) {
        	throw new NotFoundException(" Credentials do not match. ");
        }
        
        List<User> following = current.getFollowing();
        if(following != null && following.contains(toUnfollow)) {
        	following.remove(toUnfollow);
        }
        else if(!following.contains(toUnfollow)) {
        	throw new BadRequestException("You are not following this user.");
        }
        current.setFollowing(following);

        List<User> followers = toUnfollow.getFollowers();
        if(followers != null && followers.contains(current)) {
        	followers.remove(current);
        }
        toUnfollow.setFollowers(followers);
        
        userRepository.flush();
    	
    }

    @Override
    public List<TweetResponseDto> getTweetsByUsername(String username) {
        User thisUser = getUserHelper(username);
        List<Tweet> userTweets = thisUser.getTweets();
        userTweets.sort(Comparator.comparing(Tweet::getPosted).reversed());

        return tweetMapper.entitiesToDtos(userTweets);

    }

    @Override
    public List<UserResponseDto> getFollowing(String username) {
        User thisUser = getUserHelper(username);
        List<User> followers = thisUser.getFollowing();
        List<User> followersSafeCopy = new ArrayList<>(followers);

        followersSafeCopy.removeIf(User::isDeleted);

        return userMapper.entitiesToDtos(followersSafeCopy);

    }

    @Override
    public List<TweetResponseDto> getFeed(String username) {
        User current = new User();
        List<TweetResponseDto> tweets = new ArrayList<>();
        for (User u : userRepository.findAll()) {
            if (userMapper.entityToDto(u).getUsername().equals(username) && !u.isDeleted()) {
                current = u;
            }
        }

        if (userMapper.entityToDto(current).getUsername() == null) {
            throw new NotFoundException("No user with the given username.");
        }

        for (Tweet t : current.getTweets()) {
            if (!t.isDeleted()) {
                tweets.add(tweetMapper.entityToDto(t));
            }
        }

        for (User u : current.getFollowing()) {
            for (Tweet t : u.getTweets()) {
                if (!t.isDeleted()) {
                    tweets.add(tweetMapper.entityToDto(t));
                }
            }

        }
        Collections.sort(tweets, Comparator.comparing(TweetResponseDto::getPosted, Comparator.reverseOrder()));
        return tweets;

    }

    @Override
    public List<UserResponseDto> getFollowers(String username) {
        User current = new User();
        List<UserResponseDto> users = new ArrayList<>();
        for (User u : userRepository.findAll()) {
            if (userMapper.entityToDto(u).getUsername().equals(username) && !u.isDeleted()) {
                current = u;
            }
        }

        if (userMapper.entityToDto(current).getUsername() == null) {
            throw new NotFoundException("No user with the given username.");
        }

        for (User u : current.getFollowers()) {
            if (!u.isDeleted()) {
                users.add(userMapper.entityToDto(u));
            }
        }

        Collections.sort(users, Comparator.comparing(UserResponseDto::getJoined, Comparator.reverseOrder()));
        return users;

    }

    @Override
    public void followUser(String username, CredentialsDto credentialsDto) {
        User user = getUserHelper(credentialsDto.getUsername());
        User userToFollow = getUserHelper(username);

        if (!user.getCredentials().getPassword().equals(credentialsDto.getPassword())) {
            throw new NotAuthorizedException("Not authorized");
        } else if (user.getCredentials().getUsername().equals(username)) {
            throw new BadRequestException("You cannot follow yourself");
        } else if (user.getFollowing().contains(userToFollow)) {
            throw new BadRequestException("You are already following " + username);
        }
        user.getFollowing().add(userToFollow);
        userToFollow.getFollowers().add(user);
        userRepository.saveAndFlush(userToFollow);
        userRepository.saveAndFlush(user);

    }
    
    @Override
    public UserResponseDto updateUserProfile(String username, UserRequestDto userRequestDto) {
  	  Optional<User> foundUser = userRepository.findByCredentials_Username(username);
  	  if(foundUser.isEmpty()) {
  		  throw new NotFoundException("User not found");
  	  	}
  	  User user = foundUser.get();
  	  
  	  if(user.isDeleted() == true) {
  		  throw new NotFoundException("User with this username not found.");
  	  }
  	  
  	CredentialsDto checkCred = userRequestDto.getCredentials();
  		if(checkCred == null) {
  			throw new NotAuthorizedException("Not authorized");
  		}
  	  
  	  if (!user.getCredentials().getPassword().equals(checkCred.getPassword())) {
  		  throw new NotAuthorizedException("Not authorized");
  	  } 
  	  if (!user.getCredentials().getUsername().equals(checkCred.getUsername())) {
  		  throw new BadRequestException("invalid");
        }
  	  
  	 // Update the user's profile information with the provided profile details.
      ProfileDto profileDto = userRequestDto.getProfile();
      if (profileDto == null) {
    	  throw new BadRequestException("invalid");
      }
      if (profileDto.getFirstName() != null) {
          user.getProfile().setFirstName(profileDto.getFirstName());
      }
      if (profileDto.getLastName() != null) {
      user.getProfile().setLastName(profileDto.getLastName());
      }
      if (profileDto.getEmail() != null) {
      user.getProfile().setEmail(profileDto.getEmail());
      }
      if (profileDto.getPhone() != null) {
      user.getProfile().setPhone(profileDto.getPhone());
      }
      // Save the updated user to the repository.
      User updatedUser = userRepository.save(user);

      // Convert the updated user entity back to a DTO to return.
      return userMapper.entityToDto(updatedUser);
   
    }
}