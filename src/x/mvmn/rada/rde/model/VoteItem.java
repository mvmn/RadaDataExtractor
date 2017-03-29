package x.mvmn.rada.rde.model;

public class VoteItem {
	protected final int factionId;
	protected final int mpId;
	protected final String mpName;
	protected final String voteCast;

	public VoteItem(int factionId, int mpId, String mpName, String voteCast) {
		this.factionId = factionId;
		this.mpId = mpId;
		this.mpName = mpName;
		this.voteCast = voteCast;
	}

	public int getFactionId() {
		return factionId;
	}

	public int getMpId() {
		return mpId;
	}

	public String getMpName() {
		return mpName;
	}

	public String getVoteCast() {
		return voteCast;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("VoteItem [factionId=").append(factionId).append(", mpId=").append(mpId).append(", mpName=").append(mpName).append(", voteCast=")
				.append(voteCast).append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + factionId;
		result = prime * result + mpId;
		result = prime * result + ((mpName == null) ? 0 : mpName.hashCode());
		result = prime * result + ((voteCast == null) ? 0 : voteCast.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		VoteItem other = (VoteItem) obj;
		if (factionId != other.factionId)
			return false;
		if (mpId != other.mpId)
			return false;
		if (mpName == null) {
			if (other.mpName != null)
				return false;
		} else if (!mpName.equals(other.mpName))
			return false;
		if (voteCast == null) {
			if (other.voteCast != null)
				return false;
		} else if (!voteCast.equals(other.voteCast))
			return false;
		return true;
	}
}
