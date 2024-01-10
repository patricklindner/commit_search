## Usage

Clone a git repository to your disk and run the Main class, providing the absolute path of that repo as first program
argument. It will extract all java comments which have ever been mad in the git history into a csv file.

### Memory space

The regex for extracting multi line comments, can lead to a stack overflow exception.
To prevent this, configure JVM as follows: `-Xss515m -Xms16g -Xmx16g` for good measure.