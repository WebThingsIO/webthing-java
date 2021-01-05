# webthing Changelog

## [Unreleased]

## [0.14.0] - 2021-01-05
### Added
- Parameter to disable host validation in server.

## [0.13.0] - 2020-09-23
### Changed
- Update author and URLs to indicate new project home.
- Changed package name from `org.mozilla.iot` to `io.webthings`.

## [0.12.3] - 2020-06-18
### Changed
- mDNS record now indicates TLS support.

## [0.12.2] - 2020-05-04
### Changed
- Invalid POST requests to action resources now generate an error status.

## [0.12.1] - 2020-03-27
### Added
- Support OPTIONS requests to allow for CORS.

## [0.12.0] - 2019-07-12
### Changed
- Things now use `title` rather than `name`.
- Things now require a unique ID in the form of a URI.
### Added
- Ability to set a base URL path on server.
- Support for `id`, `base`, `security`, and `securityDefinitions` keys in thing description.

## [0.11.0] - 2019-01-16
### Changed
- WebThingServer constructor can now take a list of additional API routes.
### Fixed
- Properties could not include a custom `links` array at initialization.

## [0.10.0] - 2018-11-30
### Changed
- Property, Action, and Event description now use `links` rather than `href`. - [Spec PR](https://github.com/WebThingsIO/wot/pull/119)

[Unreleased]: https://github.com/WebThingsIO/webthing-java/compare/v0.14.0...HEAD
[0.14.0]: https://github.com/WebThingsIO/webthing-java/compare/v0.13.0...v0.14.0
[0.13.0]: https://github.com/WebThingsIO/webthing-java/compare/v0.12.3...v0.13.0
[0.12.3]: https://github.com/WebThingsIO/webthing-java/compare/v0.12.2...v0.12.3
[0.12.2]: https://github.com/WebThingsIO/webthing-java/compare/v0.12.1...v0.12.2
[0.12.1]: https://github.com/WebThingsIO/webthing-java/compare/v0.12.0...v0.12.1
[0.12.0]: https://github.com/WebThingsIO/webthing-java/compare/v0.11.0...v0.12.0
[0.11.0]: https://github.com/WebThingsIO/webthing-java/compare/v0.10.0...v0.11.0
[0.10.0]: https://github.com/WebThingsIO/webthing-java/compare/v0.9.1...v0.10.0
