package com.cooksys.socialmedia.services.impl;

import com.cooksys.socialmedia.dtos.*;
import com.cooksys.socialmedia.entities.Credentials;
import com.cooksys.socialmedia.entities.Hashtag;
import com.cooksys.socialmedia.entities.Tweet;
import com.cooksys.socialmedia.entities.User;
import com.cooksys.socialmedia.exceptions.BadRequestException;
import com.cooksys.socialmedia.exceptions.NotAuthorizedException;
import com.cooksys.socialmedia.exceptions.NotFoundException;
import com.cooksys.socialmedia.mappers.CredentialsMapper;
import com.cooksys.socialmedia.mappers.HashtagMapper;
import com.cooksys.socialmedia.mappers.TweetMapper;
import com.cooksys.socialmedia.mappers.UserMapper;
import com.cooksys.socialmedia.repositories.HashtagRepository;
import com.cooksys.socialmedia.repositories.TweetRepository;
import com.cooksys.socialmedia.repositories.UserRepository;
import com.cooksys.socialmedia.services.TweetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;



@Service
@RequiredArgsConstructor
public class TweetServiceImpl implements TweetService {

    private final TweetRepository tweetRepository;
    private final TweetMapper tweetMapper;
    private final UserMapper userMapper;
    private final UserRepository userRepository;
    private final CredentialsMapper credentialsMapper;
    private final HashtagRepository hashtagRepository;
    private final HashtagMapper hashtagMapper;

    @Override
    public List<TweetResponseDto> getAllTweets() {
        // Get non-deleted tweets in reverse chronological order
        List<Tweet> nonDeletedTweets = tweetRepository.findByDeletedFalseOrderByPostedDesc();
        return tweetMapper.entitiesToDtos(nonDeletedTweets);
    }

    // TODO: reimplement this once GET tweets/{id} is created
    @Override
    public TweetResponseDto deleteTweetById(Long tweetId, CredentialsDto credentialsDto) {
        Tweet tweet = tweetRepository.findById(tweetId).orElseThrow(() -> new IllegalArgumentException("Invalid tweet ID: " + tweetId));

        if (tweet.isDeleted()) {
            throw new NotFoundException("Tweet with ID: " + tweetId + " not found");
        }
        // Check if the user has permission to delete the tweet
        if (!tweet.getAuthor().getCredentials().getUsername().equals(credentialsDto.getUsername()) || !tweet.getAuthor().getCredentials().getPassword().equals(credentialsDto.getPassword())) {
            throw new NotAuthorizedException("Unauthorized to delete tweet with ID: " + tweetId);
        }

        tweet.setDeleted(true);
        tweetRepository.save(tweet);

        return tweetMapper.entityToDto(tweet);
    }
    
    // TODO: reimplement this once GET tweets/{id} is created
    @Override
    public List<UserResponseDto> getUsersMentionedByTweetId(Long tweetId) {
        Tweet tweet = tweetRepository.findById(tweetId).orElseThrow(() -> new IllegalArgumentException("Invalid tweet ID: " + tweetId));

        if (tweet.isDeleted()) {
            throw new NotFoundException("Tweet with ID: " + tweetId + " not found");
        }

        List<User> allMentionedUsers = tweet.getMentionedUsers();

        if (allMentionedUsers.isEmpty()) {
            throw new NotFoundException("No users mentioned in this tweet: " + tweetId);
        }
        ArrayList<User> mentionedUsersNotDeleted = new ArrayList<>();
        for (User user : allMentionedUsers) {
            if (user.isDeleted()) {
                continue;
            }
            mentionedUsersNotDeleted.add(user);
        }

        return userMapper.entitiesToDtos(mentionedUsersNotDeleted);
    }

    @Override
    public TweetResponseDto createRepost(Long tweetId, CredentialsDto credentials) {
        Tweet tweet = tweetRepository.findById(tweetId).orElseThrow(() -> new NotFoundException("Invalid tweet ID: " + tweetId));

        if (tweet.isDeleted()) {
            throw new NotFoundException("Tweet with ID: " + tweetId + " not found");
        }
        
        if(credentials == null || credentials.getUsername() == null || credentials.getPassword() == null) {
        	throw new BadRequestException("Bad credentials in body.");
        }
        
        Optional <User> credUser = userRepository.findByCredentials_Username(credentials.getUsername());
        
        if(credUser.isEmpty()) {
        	throw new NotFoundException("No user with these credentials was found.");
        }
        User credentU = credUser.get();
        if(!credentialsMapper.entityToDto(credentU.getCredentials()).equals(credentials)) {
        	throw new NotAuthorizedException("Credentials do not match.");
        }
        Tweet newTweet = new Tweet();
        newTweet.setAuthor(credentU);
        newTweet.setRepostOf(tweet);
        tweetRepository.saveAndFlush(newTweet);
        
        credentU.getTweets().add(newTweet);
        
        userRepository.flush();
        
        return tweetMapper.entityToDto(newTweet);
    }

    @Override
    public ContextDto getContext(Long tweetId) {
        Tweet tweet = tweetRepository.findById(tweetId).orElseThrow(() -> new NotFoundException("Invalid tweet ID: " + tweetId));

        if (tweet.isDeleted()) {
            throw new NotFoundException("Tweet with ID: " + tweetId + " not found");
        }

        ContextDto context = new ContextDto();
        List<TweetResponseDto> replyTos = new ArrayList<>();
        List<TweetResponseDto> replies = new ArrayList<>();

        TweetResponseDto tweetResponseDto = tweetMapper.entityToDto(tweet);
        getInReplyToHelper(tweetResponseDto, replyTos);
        getRepliesHelper(tweet.getReplies(), replies);

        context.setTarget(tweetResponseDto);
        context.setBefore(replyTos);
        context.setAfter(replies);
        
        return context;
    }

    private void getInReplyToHelper(TweetResponseDto tweet, List<TweetResponseDto> replyTos) {
        Tweet t = tweetMapper.dtoToEntity(tweet.getInReplyTo());
        if (t == null) {
            return;
        }
        replyTos.add(tweet.getInReplyTo());
        getInReplyToHelper(tweet.getInReplyTo(), replyTos);
    }

    private void getRepliesHelper(List<Tweet> tweets, List<TweetResponseDto> replies) {
        if (tweets != null) {
            for (Tweet t : tweets) {
                if (t.isDeleted()) {
                    continue;
                }
                replies.add(tweetMapper.entityToDto(t));
                getRepliesHelper(t.getReplies(), replies);
            }
        }
    }
  
  @Override
  public TweetResponseDto getTweetById(Long id) {
	  
	  Optional<Tweet> current = tweetRepository.findById(id);
	  
	  if(current.isEmpty()) {
		  throw new NotFoundException("Tweet not found.");
	  }
	  
	  return tweetMapper.entityToDto(current.get());
  }
  
  @Override
  public List<TweetResponseDto> getRepostsById(Long id){
	  Optional<Tweet> current = tweetRepository.findById(id);
	  
	  if(current.isEmpty()) {
		  throw new NotFoundException("Tweet not found.");
	  }
	  
	  Tweet finall = current.get();
	  
	  if(finall.isDeleted()) {
		  throw new NotFoundException("Tweet not found.");
	  }
	  
	  List<TweetResponseDto> repostList = new ArrayList<>();
	  
	  for(Tweet t: finall.getReposts()) {
		  if(t.isDeleted() == false) {
			  repostList.add(tweetMapper.entityToDto(t));
		  }
	  }
	  
	  return repostList;
  }
  
  @Override
  public List<UserResponseDto> getLikesById(Long id){
	  Optional<Tweet> current = tweetRepository.findById(id);
	  
	  if(current.isEmpty()) {
		  throw new NotFoundException("Tweet not found.");
	  }
	  
	  Tweet finall = current.get();
	  
	  List<UserResponseDto> userList = new ArrayList<>();
	  for(User u: userRepository.findByLikedTweets(finall)) {
		  if(u.isDeleted()==false) {
			  userList.add(userMapper.entityToDto(u));
		  }
	  }
	  return userList;
  }
  
  @Override
  public TweetResponseDto createReply(Long id, TweetRequestDto tweetRequest) {
  	Tweet current = new Tweet();
  	CredentialsDto credentials = tweetRequest.getCredentials();
  	Optional<Tweet> checker = tweetRepository.findById(id);
  	User author = new User();
  	if(checker.isEmpty()) {
  		throw new NotFoundException("Tweet with this id not found.");
  	}
  	for(User u: userRepository.findAll()) {
  		if(credentialsMapper.entityToDto(u.getCredentials()).equals(credentials) && u.isDeleted() == false){
  			author = u;
  		}
  	}
  	
  	if(author.getCredentials() == null) {
 		throw new NotAuthorizedException("Credentials are not correct.");
  	}
  	
  	Tweet checkerTweet = checker.get();
  	if(checkerTweet.isDeleted() == true)
  	{
  		throw new NotFoundException("Tweet with this id not found.");
  	}
  	if(tweetRequest.getContent() == null) {
  		throw new BadRequestException("Content needs to be filled in.");
  	}
  	
  	
 	String contenter = tweetRequest.getContent();
  	
  	int tracker = 0;
  	boolean starter = false;
  	
  	List<String> special = new ArrayList<>();
  	
  	for(int i = 0; i < contenter.length(); i++) {
  		if(contenter.charAt(i) == '#' || contenter.charAt(i) == '@') {
  			tracker = i;
  			starter = true;
  		}
  		else if(!Character.isLetter(contenter.charAt(i)) && !Character.isDigit(contenter.charAt(i)) && contenter.charAt(i) != '_') {
  			if(starter == true) {
  				special.add(contenter.substring(tracker, i));
  				starter = false;
  			}
  			
  		}
  		
  	}
  	
  	if(starter == true) {
  		special.add(contenter.substring(tracker, contenter.length()));
  	}

  	current.setAuthor(author);
  	current.setContent(tweetRequest.getContent());
  	current.setInReplyTo(checkerTweet);
  	
  	for(String u: special) {
  		if(u.charAt(0) == '#') {
  			Hashtag h = new Hashtag();
  			for(Hashtag hasher: hashtagRepository.findAll()) {
  				if(hasher.getLabel().equals(u.substring(1))) {
  					h = hasher;
  				}
  			}
  			if(h.getLabel() == null)
  			{
  				h.setLabel(u.substring(1));
  				h.getTweets().add(current);
  				hashtagRepository.saveAndFlush(h);
  			}
  		  	current.getHashtags().add(h);
  		}
  		else if(u.charAt(0) == '@') {
  			String label = u.substring(1, u.length());
  			for(User use: userRepository.findAll()) {
  				if(use.getCredentials().getUsername().equals(label) && use.isDeleted() == false) {
  					current.getMentionedUsers().add(use);
  				}
  			}
  		}
  	}
  	
  	return tweetMapper.entityToDto(tweetRepository.saveAndFlush(current));
  }
  
  // we need two functions, first function needs to take a string hashtag and then check if the hashtag exists, if it does return it, if not create it and then return created hashtag.  second function, check if user exists and if does return user, if it doesn't return null
  
  private Hashtag createReturnHashtag(String hash) {
		Optional<Hashtag> foundHashtag = hashtagRepository.findByLabel(hash);
		if(foundHashtag.isPresent()) {
			return foundHashtag.get();
		}
		Hashtag createdHashtag = new Hashtag();
		createdHashtag.setLabel(hash);
		hashtagRepository.saveAndFlush(createdHashtag);
		return createdHashtag;
  }
  
  private Optional<User> checkUser(String username) {
	  	String updatedUsername = username.substring(1);
		Optional<User> foundUser = userRepository.findByCredentials_Username(updatedUsername);
	return foundUser;
  }
  @Override
  public TweetResponseDto postTweet(TweetRequestDto tweetRequest) {

	  ArrayList<Hashtag> hashtagWords = new ArrayList<Hashtag>();
	  
	  ArrayList<User> mentionUsers = new ArrayList<User>();
	  
	  if(tweetRequest.getContent() == null) {
		  throw new BadRequestException("No content in the body.");
	  }
	  
	  String[] words = tweetRequest.getContent().split(" ");
	  for (String word : words) {
		  
          if (word.startsWith("#")) {
        	  hashtagWords.add(createReturnHashtag(word.substring(1)));
        	  	
          }
        	  if (word.startsWith("@")) {
        		  Optional<User> foundUser = checkUser(word);
        		  if(foundUser.isPresent()) {
        				mentionUsers.add(foundUser.get());
        			}
          }
	  }
          

  	Tweet current = new Tweet();
  	CredentialsDto credentials = tweetRequest.getCredentials();
  	if(credentials == null || credentials.getUsername() == null || credentials.getPassword() == null) {
  		throw new BadRequestException("Incorrect credential information.");
  	}
  	Optional<User> foundUser = userRepository.findByCredentials_Username(credentials.getUsername());
  	
  	if(foundUser.isEmpty()) {
  		throw new NotAuthorizedException("Credentials are empty.");
  	}
  	CredentialsDto dtoCredentials = credentialsMapper.entityToDto(foundUser.get().getCredentials());
  	if(dtoCredentials == null || dtoCredentials.getUsername() == null || dtoCredentials.getPassword() == null) {
  		throw new BadRequestException("Didn't find user.");
  	}
  	if(!credentials.getUsername().equals(dtoCredentials.getUsername())  ||  !credentials.getPassword().equals(dtoCredentials.getPassword())) {
  		
 		throw new NotAuthorizedException("Credentials are not correct.");
  	}
  	
  	if(tweetRequest.getContent() == null) {
  		throw new BadRequestException("Content needs to be filled in.");
  	}


  	current.setAuthor(foundUser.get());
  	current.setContent(tweetRequest.getContent());
  	current.setHashtags(hashtagWords);
  	current.setMentionedUsers(mentionUsers);

  	return tweetMapper.entityToDto(tweetRepository.saveAndFlush(current));
  }
  
  @Override
  public List<HashtagResponseDto> getTagsByTweetId(Long tweetId) {
	  Tweet tweet = tweetRepository.findById(tweetId).orElseThrow(() -> new IllegalArgumentException("Invalid tweet ID: " + tweetId));
	  if (tweet.isDeleted()) {
          throw new NotFoundException("Tweet with ID: " + tweetId + " not found");
      }
      List<Hashtag> allTags = tweet.getHashtags();

      if (allTags.isEmpty()) {
          throw new NotFoundException("No tags found in this tweet: " + tweetId);
      }
      return hashtagMapper.entitiesToDtos(allTags);
  }
  
  public List<TweetResponseDto> getTweetByUserMentions(String username) {
	  Optional<User> foundUser = userRepository.findByCredentials_Username(username);
	  if(foundUser.isEmpty()) {
	  		throw new NotFoundException("User not found");
	  	}
	  List<Tweet> userResults = tweetRepository.findByMentionedUsersAndDeletedFalseOrderByPostedDesc(foundUser.get());
	  return tweetMapper.entitiesToDtos(userResults);
  }
  
  @Override
  public List<TweetResponseDto> getTweetReplies(Long id){
	  Optional<Tweet> tweet = tweetRepository.findById(id);
	  if(tweet.isEmpty() || tweet.get().isDeleted()) {
		  throw new NotFoundException("Tweet not found");
	  }
	  List<Tweet> replies = tweetRepository.findByInReplyToIdAndDeletedFalse(id);
	    return tweetMapper.entitiesToDtos(replies);
	    
  }
  
  public void postTweetLike(Long id, Credentials credentialsDto) {
	  Optional<User> foundUser = userRepository.findByCredentials_Username(credentialsDto.getUsername());
	  if(foundUser.isEmpty()) {
	  		throw new NotFoundException("User not found");
	  	}
	  User user = foundUser.get();
      if (!user.getCredentials().getPassword().equals(credentialsDto.getPassword())) {
          throw new NotAuthorizedException("Not authorized");
      } 
      if (!user.getCredentials().getUsername().equals(credentialsDto.getUsername())) {
          throw new BadRequestException("invalid");
      }
	  Optional<Tweet> tweet = tweetRepository.findById(id);
	  if(tweet.isEmpty() || tweet.get().isDeleted()) {
		  throw new NotFoundException("Tweet not found");
	  }
      user.getLikedTweets().add(tweet.get());
      userRepository.saveAndFlush(user);
  }

  
}