"""Materializa la documentación existente en el docs_dir virtual de MkDocs.

Los archivos .md viven en el repo (raíz, backend/*/docs, frontend/pymes/docs, .github).
Este script los copia tal cual — sin duplicarlos en disco — y genera además las
páginas de API Reference desde los specs OpenAPI ya generados por SpringDoc.

Source: docs/api/openapi-auth.json, docs/api/openapi-core.json
"""

import json
from pathlib import Path

import mkdocs_gen_files

ROOT = Path(__file__).resolve().parent.parent.parent
EXCLUDED = {"AGENTS.md", ".git", "node_modules", ".opencode", ".agents", ".ulpi", "playwright-report", "docs-site"}
SPECS = {
    "auth": ROOT / "docs/api/openapi-auth.json",
    "core": ROOT / "docs/api/openapi-core.json",
}
METHODS = {"get", "post", "put", "patch", "delete"}


def copy_existing_docs() -> None:
    for path in ROOT.rglob("*.md"):
        if any(part in EXCLUDED for part in path.relative_to(ROOT).parts):
            continue
        rel = path.relative_to(ROOT)
        # MkDocs excluye rutas que empiezan con "." — .github se sirve desde deployment/.
        if rel.parts[0] == ".github":
            rel = Path("deployment") / rel.name
        with mkdocs_gen_files.open(str(rel), "w") as f:
            f.write(path.read_text(encoding="utf-8"))


def render_api(service: str, spec_path: Path) -> str:
    data = json.loads(spec_path.read_text(encoding="utf-8"))
    paths = data.get("paths", {})
    schemas = data.get("components", {}).get("schemas", {})
    lines = [
        f"# API Reference — {service.upper()}",
        "",
        f"> Generado desde `{spec_path.relative_to(ROOT)}` (SpringDoc). "
        "No editar a mano — ver [cómo regenerar](../docs/api/README.md#regenerar-un-comando-por-servicio).",
        "",
        f"- **OpenAPI**: {data.get('openapi')}",
        f"- **Operaciones**: {sum(1 for ops in paths.values() for m in ops if m in METHODS)}",
        f"- **Schemas**: {len(schemas)}",
        "",
        "## Endpoints",
        "",
    ]
    for path, ops in sorted(paths.items()):
        for method, op in ops.items():
            if method in METHODS:
                summary = op.get("summary", "")
                lines.append(f"- `{method.upper()}` `{path}` — {summary}".rstrip(" —"))
    lines += ["", "## Schemas", ""]
    lines += [f"- `{name}`" for name in sorted(schemas)]
    return "\n".join(lines) + "\n"


copy_existing_docs()
# Los JSON se sirven como archivos estáticos (los READMEs raíz y auth/core enlazan a ellos).
for spec_path in SPECS.values():
    with mkdocs_gen_files.open(str(spec_path.relative_to(ROOT)), "wb") as f:
        f.write(spec_path.read_bytes())
for service, spec_path in SPECS.items():
    if spec_path.exists():
        with mkdocs_gen_files.open(f"api/{service}.md", "w") as f:
            f.write(render_api(service, spec_path))
