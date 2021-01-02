package nextstep.subway.line.domain;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import nextstep.subway.line.exception.SectionException;
import nextstep.subway.station.domain.Station;

import javax.persistence.CascadeType;
import javax.persistence.Embeddable;
import javax.persistence.OneToMany;
import java.util.*;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Embeddable
public class Sections {

  public static final String ALREADY_SECTION_EXIST = "상행역과 하행역이 이미 노선에 모두 등록되어 있다면 추가할 수 없음";
  public static final String MUST_HAVING_UP_OR_DOWN_STATION = "상행역과 하행역 둘 중 하나도 포함되어있지 않으면 추가할 수 없음";

  @OneToMany(mappedBy = "line", cascade = CascadeType.ALL)
  private List<Section> sections = new ArrayList<>();

  public Sections(Section upSection, Section downSection) {
    this.sections = Arrays.asList(upSection, downSection);
  }

  public void addSection(Section newSection) {
    boolean isIncludeUpStation = isIncludeStationList(newSection.getUpStation());
    boolean isIncludeDownStation = isIncludeStationList(newSection.getDownStation());

    validateSection(isIncludeUpStation, isIncludeDownStation);

    if (isIncludeUpStation) {
      updateSectionWhenEqualUpStation(newSection);
    }

    if (isIncludeDownStation) {
      updateSectionWhenEqualDownStation(newSection);
    }

    this.sections.add(newSection);
  }

  public void updateSectionWhenEqualUpStation(Section newSection) {
    this.sections.stream()
        .filter(section -> section.isUpStationInSection(newSection.getUpStation()))
        .findFirst()
        .ifPresent(section -> section.updateUpToDown(newSection.getDownStation(), newSection.getDistance()));
  }

  public void updateSectionWhenEqualDownStation(Section newSection) {
    this.sections.stream()
        .filter(section -> section.isDownStationInSection(newSection.getDownStation()))
        .findFirst()
        .ifPresent(section -> section.updateDownToUp(newSection.getUpStation(), newSection.getDistance()));
  }

  public List<Section> getOrderedSections() {
    // 출발지점 찾기
    Optional<Section> preLineStation = sections.stream()
        .filter(it -> it.getUpStation() == null)
        .findFirst();

    List<Section> result = new ArrayList<>();
    while (preLineStation.isPresent()) {
      Section preStation = preLineStation.get();
      result.add(preStation);
      preLineStation = sections.stream()
          .filter(it -> it.getUpStation() == preStation.getDownStation())
          .findFirst();
    }
    return result;
  }

  public List<Station> getStations() {
    return this.getOrderedSections().stream()
        .map(Section::getDownStation)
        .collect(Collectors.toList());
  }

  private void validateSection(boolean isIncludeUpStation, boolean isIncludeDownStation) {
    if (isIncludeUpStation && isIncludeDownStation) {
      throw new SectionException(ALREADY_SECTION_EXIST);
    }
    if (!isIncludeUpStation && !isIncludeDownStation) {
      throw new SectionException(MUST_HAVING_UP_OR_DOWN_STATION);
    }
  }

  private boolean isIncludeStationList(Station station) {
    return this.sections.stream()
        .anyMatch(section -> section.isDownStationInSection(station));
  }
}
