package x.mvmn.rada.rde.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VotingInfo {

	protected final Map<Integer, String> factions = new HashMap<>();

	protected final List<VoteItem> votes = new ArrayList<>();

	protected Date voteDateTime;

	public Map<Integer, String> getFactions() {
		return factions;
	}

	public List<VoteItem> getVotes() {
		return votes;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("VoteSessionInfo [factions=").append(factions).append(", votes=").append(votes).append("]");
		return builder.toString();
	}

	public Date getVoteDateTime() {
		return voteDateTime;
	}

	public void setVoteDateTime(Date voteDateTime) {
		this.voteDateTime = voteDateTime;
	}
}
