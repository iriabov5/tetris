.PHONY: install check backend-check frontend-check e2e api web javadoc openapi api-generate

install:
	cd frontend && npm ci

check: backend-check frontend-check

backend-check:
	cd backend && ./mvnw -B -ntp -s .mvn/central-settings.xml verify

frontend-check:
	cd frontend && npm run api:check && npm run typecheck && npm run lint && npm run format:check && npm test && npm run test:codegen && npm run build

e2e:
	cd frontend && npm run test:e2e
	node scripts/verify-ui.mjs

api:
	java -jar backend/api/target/api-1.0.0-SNAPSHOT.jar

web:
	cd frontend && npm run dev

javadoc:
	cd backend && ./mvnw -B -ntp -s .mvn/central-settings.xml test-compile javadoc:javadoc-no-fork javadoc:test-javadoc-no-fork

# Временный SpringBootTest-сервер использует обычное поле и свободный порт.
openapi:
	cd backend && ./mvnw -B -ntp -s .mvn/central-settings.xml -pl api -am test -Dtest=OpenApiContractHttpTest -Dsurefire.failIfNoSpecifiedTests=false -Dtetris.openapi.export=true -Dsurefire.timeout=60

# Экспорт Java-контракта и генерация клиентских методов/моделей.
api-generate: openapi
	cd frontend && npm run api:generate
