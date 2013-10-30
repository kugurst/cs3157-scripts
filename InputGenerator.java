public abstract class InputGenerator
{
	/** The next line of input to send to the process. This method should block if the InputGenerator
	 * is still decided what to send. It should return null if it is done sending input to the
	 * process. It should return a String otherwise.
	 * @return null to indicate the end of input. A String otherwise. */
	public abstract String getNextInput();

	/** Used to implement state based behavior in the InputGenerator. The generator can decide what
	 * to do with the output from the process. This method should not take long, as the LineReader
	 * that called this method needs to return to handle the next line of output from the program.
	 * @param line
	 *            The next line of output from the program. */
	public void putNextStdOut(String line)
	{}

	public void putNextStdErr(String line)
	{}
}
