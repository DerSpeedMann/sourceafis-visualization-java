// Part of SourceAFIS Visualization: https://sourceafis.machinezoo.com/transparency/
package com.machinezoo.sourceafis.visualization;

import static java.util.stream.Collectors.*;
import java.awt.*;
import java.util.*;
import java.util.List;
import com.machinezoo.pushmode.dom.*;
import com.machinezoo.sourceafis.transparency.types.*;
import com.machinezoo.sourceafis.visualization.utils.*;
import it.unimi.dsi.fastutil.objects.*;
import one.util.streamex.*;

public class TransparencyMarkers {
	/*
	 * Method naming:
	 * - mark*() - discrete SVG markers with lots of transparency around them
	 * - paint*() - opaque image (usually pixmap), useful only as a base layer
	 * - overlay*() - semi-transparent image (usually pixmap)
	 * - embed*() - pixmap as an SVG image element
	 * - *Diff() - visual diff relative to previous (or other) stage
	 * - *ModelName*() - renders ModelName (e.g. BooleanMatrix)
	 * - *KeyName*() - appropriately renders data from particular transparency key (e.g. EqualizedImage, may be abbreviated)
	 * 
	 * Methods are sorted in the same order in which corresponding data is produced by the algorithm.
	 * All generic and helper methods are defined just before the first method that uses them.
	 * The first parameter is the object to be visualized.
	 * Following parameters contain additional context needed for visualization
	 * in the same order in which they are generated by the algorithm.
	 */
	public static DomContent markRectWeight(double weight, IntRect rect) {
		double radius = Math.sqrt(weight) * IntRects.radius(rect);
		var center = IntRects.center(rect);
		return Svg.circle()
			.cx(center.x())
			.cy(center.y())
			.r(radius)
			.stroke("#080")
			.strokeWidth(0.3)
			.fill("#0f0")
			.fillOpacity(0.2);
	}
	public static DomContent markBlockWeight(DoubleMatrix matrix, BlockMap blocks) {
		DoubleSummaryStatistics stats = Arrays.stream(matrix.cells()).summaryStatistics();
		DomFragment markers = new DomFragment();
		for (IntPoint at : IntPoints.stream(blocks.primary().blocks())) {
			double weight = (matrix.get(at) - stats.getMin()) / (stats.getMax() - stats.getMin());
			markers.add(markRectWeight(weight, blocks.primary().block(at)));
		}
		return markers;
	}
	public static DomContent markContrast(DoubleMatrix contrast, BlockMap blocks) {
		return markBlockWeight(contrast, blocks);
	}
	public static TransparencyPixmap paintBooleanMatrix(BooleanMatrix matrix, int foreground, int background) {
		TransparencyPixmap writable = new TransparencyPixmap(matrix.size());
		writable.fill(background);
		for (IntPoint at : IntPoints.stream(matrix.size()))
			if (matrix.get(at))
				writable.set(at, foreground);
		return writable;
	}
	public static DomContent embedPng(TransparencyPixmap pixmap) {
		return Svg.image()
			.width(pixmap.width)
			.height(pixmap.height)
			.href("data:image/png;base64," + Base64.getEncoder().encodeToString(pixmap.png()));
	}
	public static DomContent embedJpeg(TransparencyPixmap pixmap) {
		return Svg.image()
			.width(pixmap.width)
			.height(pixmap.height)
			.href("data:image/jpeg;base64," + Base64.getEncoder().encodeToString(pixmap.jpeg()));
	}
	public static TransparencyPixmap overlayMask(BooleanMatrix mask) {
		return paintBooleanMatrix(mask, 0x20_ff_ff_00, 0x20_00_ff_ff);
	}
	public static TransparencyPixmap overlayMask(BooleanMatrix mask, BlockMap blocks) {
		return overlayMask(mask.expand(blocks));
	}
	public static TransparencyPixmap overlayAbsoluteContrastMask(BooleanMatrix mask, BlockMap blocks) {
		return overlayMask(mask, blocks);
	}
	public static TransparencyPixmap overlayRelativeContrastMask(BooleanMatrix mask, BlockMap blocks) {
		return overlayMask(mask, blocks);
	}
	public static TransparencyPixmap overlayCombinedMask(BooleanMatrix mask, BlockMap blocks) {
		return overlayMask(mask, blocks);
	}
	public static TransparencyPixmap overlayFilteredMask(BooleanMatrix mask, BlockMap blocks) {
		return overlayMask(mask, blocks);
	}
	private static TransparencyPixmap paintPixelwiseOrientation(DoublePointMatrix orientations, int opacity) {
		opacity = opacity << 24;
		TransparencyPixmap pixmap = new TransparencyPixmap(orientations.size());
		/*
		 * Transparent white, so that the result will render correctly as both JPEG and PNG.
		 */
		pixmap.fill(0x00_ff_ff_ff);
		double max = Math.log1p(IntPoints.stream(orientations.size()).map(orientations::get).mapToDouble(DoublePoints::length).max().orElse(1));
		for (IntPoint at : IntPoints.stream(orientations.size())) {
			DoublePoint vector = orientations.get(at);
			if (vector.x() != 0 || vector.y() != 0) {
				double angle = vector.angle();
				double strength = Math.log1p(DoublePoints.length(vector)) / max;
				pixmap.set(at, Color.HSBtoRGB((float)(angle / DoubleAnglesEx.PI2), (float)(0.2 + 0.8 * strength), 1.0f) & 0xffffff | opacity);
			}
		}
		return pixmap;
	}
	public static TransparencyPixmap paintPixelwiseOrientation(DoublePointMatrix orientations) {
		return paintPixelwiseOrientation(orientations, 0xff);
	}
	public static TransparencyPixmap overlayPixelwiseOrientation(DoublePointMatrix orientations) {
		return paintPixelwiseOrientation(orientations, 0x60);
	}
	public static DomContent markRectOrientation(DoublePoint orientation, IntRect rect) {
		DoublePoint center = IntRects.center(rect);
		DoublePoint direction = DoubleAngles.toVector(DoubleAngles.fromOrientation(orientation.angle()));
		DoublePoint arm = DoublePoints.multiply(0.5 * Math.min(rect.width(), rect.height()), direction);
		DoublePoint from = DoublePoints.sum(center, arm);
		DoublePoint to = DoublePoints.difference(center, arm);
		return Svg.line()
			.x1(from.x())
			.y1(from.y())
			.x2(to.x())
			.y2(to.y())
			.stroke("red");
	}
	public static DomContent markBlockOrientation(DoublePointMatrix orientations, BlockMap blocks, BooleanMatrix mask) {
		DomFragment markers = new DomFragment();
		for (IntPoint at : IntPoints.stream(blocks.primary().blocks()))
			if (mask == null || mask.get(at))
				markers.add(markRectOrientation(orientations.get(at), blocks.primary().block(at)));
		return markers;
	}
	public static DomContent markSmoothedOrientation(DoublePointMatrix orientations, BlockMap blocks, BooleanMatrix mask) {
		return markBlockOrientation(orientations, blocks, mask);
	}
	public static TransparencyPixmap paintBooleanMatrix(BooleanMatrix matrix) {
		return paintBooleanMatrix(matrix, 0xff_00_00_00, 0xff_ff_ff_ff);
	}
	public static TransparencyPixmap paintBinarized(BooleanMatrix binarized) {
		return paintBooleanMatrix(binarized);
	}
	public static TransparencyPixmap overlayBooleanMatrix(BooleanMatrix matrix) {
		return paintBooleanMatrix(matrix, 0x90_00_ff_ff, 0);
	}
	public static TransparencyPixmap overlayBinarized(BooleanMatrix binarized) {
		return overlayBooleanMatrix(binarized);
	}
	public static TransparencyPixmap paintBooleanMatrixDiff(BooleanMatrix previous, BooleanMatrix next) {
		TransparencyPixmap writable = new TransparencyPixmap(next.size());
		for (int y = 0; y < next.height(); ++y)
			for (int x = 0; x < next.width(); ++x) {
				boolean original = previous.get(x, y);
				boolean updated = next.get(x, y);
				if (updated)
					writable.set(x, y, original ? 0xff_00_00_00 : 0xff_00_ff_00);
				else
					writable.set(x, y, original ? 0xff_ff_00_00 : 0xff_ff_ff_ff);
			}
		return writable;
	}
	public static TransparencyPixmap paintFilteredBinary(BooleanMatrix filtered) {
		return paintBooleanMatrix(filtered);
	}
	public static TransparencyPixmap paintFilteredBinaryDiff(BooleanMatrix filtered, BooleanMatrix binarized) {
		return paintBooleanMatrixDiff(binarized, filtered);
	}
	public static TransparencyPixmap overlayPixelMask(BooleanMatrix mask) {
		return overlayMask(mask);
	}
	public static TransparencyPixmap overlayInnerMask(BooleanMatrix mask) {
		return overlayMask(mask);
	}
	public static TransparencyPixmap overlayBinarizedSkeleton(BooleanMatrix binarized) {
		return overlayBooleanMatrix(binarized);
	}
	public static TransparencyPixmap overlaySkeletonShadow(BooleanMatrix shadow) {
		return paintBooleanMatrix(shadow, 0xff_ff_00_00, 0);
	}
	public static TransparencyPixmap overlayThinned(BooleanMatrix thinned) {
		return overlaySkeletonShadow(thinned);
	}
	public static TransparencyPixmap overlaySkeletonShadow(SkeletonGraph skeleton) {
		return overlaySkeletonShadow(SkeletonGraphs.shadow(skeleton));
	}
	private static DomContent markSkeletonMinutia(IntPoint minutia, String color) {
		DoublePoint at = IntPoints.center(minutia);
		return Svg.circle()
			.cx(at.x())
			.cy(at.y())
			.r(4)
			.fill("none")
			.stroke(color)
			.strokeWidth(0.7);
	}
	public static DomContent markSkeletonMinutia(IntPoint minutia, Object2IntMap<IntPoint> counts) {
		return markSkeletonMinutia(minutia, counts.getOrDefault(minutia, 0) == 1 ? "blue" : "cyan");
	}
	private static Object2IntMap<IntPoint> ridgeCounts(SkeletonGraph skeleton) {
		var counts = new Object2IntOpenHashMap<IntPoint>();
		for (var ridge : skeleton.ridges()) {
			var minutia = skeleton.minutiae()[ridge.start()];
			counts.putIfAbsent(minutia, 0);
			counts.addTo(minutia, 1);
		}
		return counts;
	}
	public static DomContent markSkeleton(SkeletonGraph skeleton) {
		DomFragment markers = new DomFragment();
		markers.add(embedPng(overlaySkeletonShadow(skeleton)));
		var counts = ridgeCounts(skeleton);
		for (var minutia : skeleton.minutiae())
			markers.add(markSkeletonMinutia(minutia, counts));
		return markers;
	}
	public static DomContent markTraced(SkeletonGraph skeleton) {
		return markSkeleton(skeleton);
	}
	public static DomContent markAddedSkeletonMinutia(IntPoint minutia) {
		return markSkeletonMinutia(minutia, "green");
	}
	public static DomContent markRemovedSkeletonMinutia(IntPoint minutia) {
		return markSkeletonMinutia(minutia, "red");
	}
	public static DomContent paintSkeletonDiff(SkeletonGraph previous, SkeletonGraph next) {
		DomFragment markers = new DomFragment();
		markers.add(embedPng(paintBooleanMatrixDiff(SkeletonGraphs.shadow(previous), SkeletonGraphs.shadow(next))));
		var counts = ridgeCounts(next);
		Set<IntPoint> previousMinutiae = Arrays.stream(previous.minutiae()).collect(toSet());
		Set<IntPoint> currentMinutiae = Arrays.stream(next.minutiae()).collect(toSet());
		for (var minutia : previous.minutiae())
			if (!currentMinutiae.contains(minutia))
				markers.add(markRemovedSkeletonMinutia(minutia));
		for (var minutia : next.minutiae()) {
			if (!previousMinutiae.contains(minutia))
				markers.add(markAddedSkeletonMinutia(minutia));
			else
				markers.add(markSkeletonMinutia(minutia, counts));
		}
		return markers;
	}
	public static DomContent markDots(SkeletonGraph skeleton) {
		return markSkeleton(skeleton);
	}
	public static DomContent paintDotsDiff(SkeletonGraph dots, SkeletonGraph traced) {
		return paintSkeletonDiff(traced, dots);
	}
	public static DomContent markPores(SkeletonGraph skeleton) {
		return markSkeleton(skeleton);
	}
	public static DomContent paintPoresDiff(SkeletonGraph pores, SkeletonGraph removedDots) {
		return paintSkeletonDiff(removedDots, pores);
	}
	public static DomContent markGaps(SkeletonGraph skeleton) {
		return markSkeleton(skeleton);
	}
	public static DomContent paintGapsDiff(SkeletonGraph gaps, SkeletonGraph pores) {
		return paintSkeletonDiff(pores, gaps);
	}
	public static DomContent markTails(SkeletonGraph skeleton) {
		return markSkeleton(skeleton);
	}
	public static DomContent paintTailsDiff(SkeletonGraph tails, SkeletonGraph gaps) {
		return paintSkeletonDiff(gaps, tails);
	}
	public static DomContent markFragments(SkeletonGraph skeleton) {
		return markSkeleton(skeleton);
	}
	public static DomContent paintFragmentsDiff(SkeletonGraph fragments, SkeletonGraph tails) {
		return paintSkeletonDiff(tails, fragments);
	}
	private static DomContent markMinutia(MinutiaPoint minutia, String color) {
		DoublePoint at = MinutiaPoints.center(minutia);
		return Svg.g()
			.add(Svg.circle()
				.cx(0)
				.cy(0)
				.r(3.5)
				.fill("none")
				.stroke(color))
			.add(Svg.line()
				.x1(3.5)
				.y1(0)
				.x2(10)
				.y2(0)
				.stroke(color))
			.transform("translate(" + at.x() + " " + at.y() + ") rotate(" + DoubleAngles.degrees(minutia.direction()) + ")");
	}
	public static DomContent markMinutia(MinutiaPoint minutia) {
		return markMinutia(minutia, minutia.type() == MinutiaType.ENDING ? "blue" : "green");
	}
	public static DomContent markTemplate(Template template) {
		DomFragment markers = new DomFragment();
		for (var minutia : template.minutiae())
			markers.add(markMinutia(minutia));
		return markers;
	}
	public static DomContent markSkeletonMinutiae(Template minutiae) {
		return markTemplate(minutiae);
	}
	public static DomContent markRemovedMinutia(MinutiaPoint minutia) {
		return markMinutia(minutia, "red");
	}
	public static DomContent markTemplateDiff(Template previous, Template next) {
		DomFragment markers = new DomFragment();
		Set<IntPoint> positions = Arrays.stream(next.minutiae()).map(m -> m.position()).collect(toSet());
		for (var minutia : previous.minutiae())
			if (!positions.contains(minutia.position()))
				markers.add(markRemovedMinutia(minutia));
		markers.add(markTemplate(next));
		return markers;
	}
	public static DomContent markInnerMinutiae(Template minutiae) {
		return markTemplate(minutiae);
	}
	public static DomContent markInnerMinutiaeDiff(Template inner, Template skeleton) {
		return markTemplateDiff(skeleton, inner);
	}
	public static DomContent markClouds(Template minutiae) {
		return markTemplate(minutiae);
	}
	public static DomContent markCloudsDiff(Template removedClouds, Template inner) {
		return markTemplateDiff(inner, removedClouds);
	}
	public static DomContent markTopMinutiae(Template minutiae) {
		return markTemplate(minutiae);
	}
	public static DomContent markTopMinutiaeDiff(Template top, Template removedClouds) {
		return markTemplateDiff(removedClouds, top);
	}
	public static DomContent markShuffled(Template shuffled) {
		return markTemplate(shuffled);
	}
	private static record EdgeLine(int reference, NeighborEdge edge) {
	}
	public static DomContent markMinutiaPosition(MinutiaPoint minutia) {
		DoublePoint at = MinutiaPoints.center(minutia);
		return Svg.circle()
			.cx(at.x())
			.cy(at.y())
			.r(2.5)
			.fill("red");
	}
	private static String colorEdgeShape(double length, double angle) {
		double stretch = Math.min(1, Math.log1p(length) / Math.log1p(300));
		int color = Color.HSBtoRGB((float)(angle / DoubleAnglesEx.PI2), 1.0f, (float)(1 - 0.5 * stretch));
		return String.format("#%06x", color & 0xffffff);
	}
	private static DomContent markEdgeShape(EdgeShape shape, MinutiaPoint reference, MinutiaPoint neighbor, double width) {
		DoublePoint referencePos = MinutiaPoints.center(reference);
		DoublePoint neighborPos = MinutiaPoints.center(neighbor);
		DoublePoint middle = DoublePoints.sum(DoublePoints.multiply(0.5, DoublePoints.difference(neighborPos, referencePos)), referencePos);
		return new DomFragment()
			.add(Svg.line()
				.x1(referencePos.x())
				.y1(referencePos.y())
				.x2(middle.x())
				.y2(middle.y())
				.stroke(colorEdgeShape(shape.length(), shape.referenceAngle()))
				.strokeWidth(width))
			.add(Svg.line()
				.x1(neighborPos.x())
				.y1(neighborPos.y())
				.x2(middle.x())
				.y2(middle.y())
				.stroke(colorEdgeShape(shape.length(), shape.neighborAngle()))
				.strokeWidth(width));
	}
	public static DomContent markNeighborEdge(NeighborEdge edge, int reference, Template template, boolean symmetrical) {
		return markEdgeShape(edge, template.minutiae()[reference], template.minutiae()[edge.neighbor()], symmetrical ? 1.2 : 0.8);
	}
	private static DomElement markPairingEdge(EdgePair edge, MatchSide side, Template template) {
		DoublePoint reference = MinutiaPoints.center(template.minutiae()[edge.from().side(side)]);
		DoublePoint neighbor = MinutiaPoints.center(template.minutiae()[edge.to().side(side)]);
		return Svg.line()
			.x1(reference.x())
			.y1(reference.y())
			.x2(neighbor.x())
			.y2(neighbor.y());
	}
	public static DomContent markPairingTreeEdge(EdgePair edge, MatchSide side, Template template) {
		return markPairingEdge(edge, side, template)
			.strokeWidth(2)
			.stroke("green");
	}
	public static DomContent markPairingSupportEdge(EdgePair edge, MatchSide side, Template template) {
		return markPairingEdge(edge, side, template)
			.stroke("yellow");
	}
	public static DomContent markEdges(NeighborEdge[][] edges, Template template) {
		DomFragment markers = new DomFragment();
		List<EdgeLine> sorted = IntStreamEx.range(edges.length)
			.flatMapToObj(n -> Arrays.stream(edges[n]).map(e -> new EdgeLine(n, e)))
			.sorted(Comparator.comparing(e -> -e.edge().length()))
			.collect(toList());
		for (EdgeLine line : sorted) {
			boolean symmetrical = Arrays.stream(edges[line.edge().neighbor()]).anyMatch(e -> e.neighbor() == line.reference());
			markers.add(markNeighborEdge(line.edge(), line.reference(), template, symmetrical));
		}
		for (var minutia : template.minutiae())
			markers.add(markMinutiaPosition(minutia));
		return markers;
	}
	public static DomContent markIndexedEdge(IndexedEdge edge, Template template) {
		return markEdgeShape(edge, template.minutiae()[edge.reference()], template.minutiae()[edge.neighbor()], 0.6);
	}
	public static DomContent markHash(EdgeHashEntry[] hash, Template template) {
		DomFragment markers = new DomFragment();
		List<IndexedEdge> edges = StreamEx.of(hash)
			.flatArray(e -> e.edges())
			.sorted(Comparator.comparing(e -> -e.length()))
			.collect(toList());
		for (IndexedEdge edge : edges)
			if (edge.reference() < edge.neighbor())
				markers.add(markIndexedEdge(edge, template));
		for (var minutia : template.minutiae())
			markers.add(markMinutiaPosition(minutia));
		return markers;
	}
	public static DomContent markMinutiaPositions(Template template) {
		DomFragment markers = new DomFragment();
		for (var minutia : template.minutiae())
			markers.add(markMinutiaPosition(minutia));
		return markers;
	}
	public static DomContent markRoots(MinutiaPair[] roots, Template probe, Template candidate) {
		TransparencySplit split = new TransparencySplit(probe.size(), candidate.size());
		for (MinutiaPair pair : roots) {
			DoublePoint probePos = MinutiaPoints.center(probe.minutiae()[pair.probe()]);
			DoublePoint candidatePos = MinutiaPoints.center(candidate.minutiae()[pair.candidate()]);
			split.add(Svg.line()
				.x1(split.leftX(probePos.x()))
				.y1(split.leftY(probePos.y()))
				.x2(split.rightX(candidatePos.x()))
				.y2(split.rightY(candidatePos.y()))
				.stroke("green")
				.strokeWidth(0.4));
		}
		return split.content();
	}
	public static DomContent markRoot(MinutiaPoint minutia) {
		DoublePoint at = MinutiaPoints.center(minutia);
		return Svg.circle()
			.cx(at.x())
			.cy(at.y())
			.r(3.5)
			.fill("blue");
	}
	public static DomContent markPairing(PairingGraph pairing, MatchSide side, Template template) {
		DomFragment markers = new DomFragment();
		for (var edge : pairing.support())
			markers.add(markPairingSupportEdge(edge, side, template));
		for (var edge : pairing.tree())
			markers.add(markPairingTreeEdge(edge, side, template));
		for (var minutia : template.minutiae())
			markers.add(markMinutiaPosition(minutia));
		var root = template.minutiae()[pairing.root().side(side)];
		markers.add(markRoot(root));
		return markers;
	}
}
