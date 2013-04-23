android-dailymotion-kids
========================

Android application of Dailymotion for Kids.
------------------------

### Introduction

This application allows kids to access their favourite content on Dailymotion. The application can work in two modes: free, and premium. As a free user, the kid will be able to watch a trailer of his/her favourite shows. With a premium subscription (paid monthly), he/she gets access to a world of content with episodes from many shows hosted on Dailymotion.

### Technical Architecture
The application is composed of two modules:
* *Dailymotion SDK*: A library that provides utility methods to authentify against the Dailymotion API, as well as requesting it. It notably handles the access token retrieval, its refreshing, and the requesting of various resources of the Dailymotion Graph API. An Exception class has been created to simplify the error management when communicating with Dailymotion.
* *Dailymotion Kids*: Dailymotion Kids is the Android application that gives access to premium content for kids.
