import axios from 'axios';

export function getPlayers() {
	return axios.get('/api/player');
};

export function switchTeam(player, newTeamId) {
	var url = "/player/" + player.id + "/team/" + newTeamId;
	return axios.post('/api', {
		url: url,
		data: {
			season : player.currentSeason.season,
			teamId : player.currentSeason.teamId
		}
	});
};

export function switchPosition(player, newPositionId) {
	var url = "/player/" + player.id + "/position/" + newPositionId;
	return axios.post('/api', {
		url: url,
		data: {
			season : player.currentSeason.season,
			fantasyPosition: player.currentSeason.fantasyPosition ? 
				player.currentSeason.fantasyPosition.id : 0
		}
	});
};