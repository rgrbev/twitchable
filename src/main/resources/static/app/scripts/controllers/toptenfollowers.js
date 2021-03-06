'use strict';

/**
 * @ngdoc function
 * @name twitchableFrontEndApp.controller:RandomchannelsCtrl
 * @description
 * # RandomchannelsCtrl
 * Controller of the twitchableFrontEndApp
 */
angular.module('twitchableFrontEndApp')
  .controller('TopTenFollowersCtrl', function ($scope, $http, LiveStream, $location, $cookieStore) {

	  
	  $http.get("/channel/findTopChannels")
	    .then(function(d) {
	        $scope.data = d.data;
	    });
	  
	  $scope.setStreamChannel = function(channel){
	    	$cookieStore.put("currentChannel", channel);
	    	$cookieStore.put("currentRating", channel.rating);
	    	$location.path("/video");
	    }
    
  });
