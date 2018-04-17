import net.ssehub.kernel_haven.incremental.util.diff.analyzer.DiffAnalyzer;
import net.ssehub.kernel_haven.util.null_checks.NonNull;
	public DiffFile(@NonNull DiffAnalyzer analyzer) throws IOException {
		this.changeSet = analyzer.parse();
	public Collection<FileEntry> getEntries() {
		return changeSet;