# Stream Chat Livedata & Offline

This repo adds offline support and livedata support to Stream's Chat SDK.

## Offline

Offline support is essential for a good chat user experience.
Mobile networks tend to lose connection frequently.
This package ensures you can still send messages, reactions and create new channels while offline.

## Livedata

Stream's chat API exposes a few dozen events that all update the chat state.
Messages can be created, updated and removed. Channels can be updated, muted, deleted, members can be added.
Reactions are another example.

The end result is that you need a lot of logic to keep your local chat state up to date.
This library handles all this logic for you and simply exposes Livedata objects that change.

## How it all fits together

Stream's Chat SDKs for Android have 3 libraries:

- The low level client (Make API calls and receive events)
- Livedata & offline support (this library)
- The Chat Views and Sample app

# Using this library

Here are the most common ways to use the library

## Unread counts

## Messages for a channel

## Querying channels

# Development

* Each user has it's own Room DB. Some of our API responses are user specific. One example is own_reactions on a message. so if you switch users we need to use a different database/storage for the results
* Suspend functions are only used on private methods. Public ones expose livedata objects.



# Questions/Research:

- Should we call it a StreamChatRepository or something like StreamChatUtility (it's not 100% the same as the standard repository concept)
- Converters can be very verbose is there a better way?
- How to manually trigger an event on the low level client...