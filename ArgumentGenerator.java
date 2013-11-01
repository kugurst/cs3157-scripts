public interface ArgumentGenerator
{
	/** Returns the next argument to use with the pending command.
	 * @return A {@link String} representing the next argument to be used for executing the pending
	 *         command. Should return an empty string, instead of null, to symbolize no argument. */
	public String getNextArgument();
}
