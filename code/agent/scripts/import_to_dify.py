#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""将本地 RAG 语料导入 Dify 知识库。

依赖 Dify OpenAPI，使用 create-by-text 逐文件/逐条问答创建文档。

用法：
    python import_to_dify.py --dry-run
    python import_to_dify.py
"""

from __future__ import annotations

import argparse
import json
import os
import sys
import urllib.error
import urllib.request
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
ENV_FILE = ROOT / ".env"
KNOWLEDGE_DIR = ROOT / "knowledge"
QA_FILE = ROOT / "data" / "qa_pairs.json"


def load_env() -> dict[str, str]:
    env = dict(os.environ)
    if ENV_FILE.exists():
        for line in ENV_FILE.read_text(encoding="utf-8").splitlines():
            line = line.strip()
            if not line or line.startswith("#") or "=" not in line:
                continue
            key, value = line.split("=", 1)
            env.setdefault(key.strip(), value.strip())
    return env


def request_json(url: str, api_key: str, payload: dict, timeout: int = 60) -> dict:
    data = json.dumps(payload, ensure_ascii=False).encode("utf-8")
    request = urllib.request.Request(
        url,
        data=data,
        headers={
            "Authorization": f"Bearer {api_key}",
            "Content-Type": "application/json",
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(request, timeout=timeout) as response:
            return json.loads(response.read().decode("utf-8"))
    except urllib.error.HTTPError as exc:
        detail = exc.read().decode("utf-8", errors="ignore")
        print(f"请求失败 HTTP {exc.code}: {exc.url}")
        print(detail)
        raise


def collect_documents() -> list[dict]:
    docs: list[dict] = []
    for md_file in sorted(KNOWLEDGE_DIR.glob("*.md")):
        docs.append(
            {
                "name": md_file.stem,
                "text": md_file.read_text(encoding="utf-8"),
                "kind": "markdown",
            }
        )

    if QA_FILE.exists():
        qa_items = json.loads(QA_FILE.read_text(encoding="utf-8"))
        for idx, item in enumerate(qa_items, start=1):
            question = str(item.get("question", "")).strip()
            answer = str(item.get("answer", "")).strip()
            text = f"问题：{question}\n答案：{answer}"
            docs.append(
                {
                    "name": f"QA-{idx:02d}-{question[:24]}",
                    "text": text,
                    "kind": "qa",
                }
            )
    return docs


def main() -> None:
    parser = argparse.ArgumentParser(description="导入土豆宠物之家知识库到 Dify")
    parser.add_argument("--dry-run", action="store_true", help="只预览，不调用 Dify")
    parser.add_argument("--dataset-name", default=None, help="覆盖数据集名称")
    parser.add_argument(
        "--indexing-technique",
        choices=["economy", "high_quality"],
        default=None,
        help="索引方式，默认读取 .env",
    )
    args = parser.parse_args()

    env = load_env()
    api_base = env.get("DIFY_API_BASE", "http://localhost/v1").rstrip("/")
    api_key = env.get("DIFY_API_KEY", "").strip()
    dataset_name = args.dataset_name or env.get(
        "DIFY_DATASET_NAME", "土豆宠物之家知识库"
    )
    description = env.get(
        "DIFY_DATASET_DESCRIPTION",
        "土豆宠物之家门店、服务、宠物养护与充值政策知识库",
    )
    indexing_technique = args.indexing_technique or env.get(
        "DIFY_INDEXING_TECHNIQUE", "economy"
    )

    documents = collect_documents()
    print(f"数据集名称：{dataset_name}")
    print(f"索引方式：{indexing_technique}")
    print(f"待导入文档：{len(documents)} 个")

    if args.dry_run:
        for doc in documents:
            print(f"- [{doc['kind']}] {doc['name']}")
        return

    if not api_key or "替换为" in api_key:
        print("请先在 rag/.env 中填写真实的 DIFY_API_KEY。")
        sys.exit(1)

    dataset = request_json(
        f"{api_base}/datasets",
        api_key,
        {
            "name": dataset_name,
            "description": description,
            "indexing_technique": indexing_technique,
        },
    )
    dataset_id = dataset.get("id")
    if not dataset_id:
        print(f"创建数据集失败：{dataset}")
        sys.exit(1)
    print(f"数据集已创建：{dataset_id}")

    success = 0
    failed = 0
    for idx, doc in enumerate(documents, start=1):
        try:
            result = request_json(
                f"{api_base}/datasets/{dataset_id}/document/create-by-text",
                api_key,
                {
                    "name": doc["name"],
                    "text": doc["text"],
                    "indexing_technique": indexing_technique,
                    "process_rule": {"mode": "automatic"},
                },
            )
            doc_id = result.get("document", {}).get("id")
            print(f"[{idx}/{len(documents)}] 已导入：{doc['name']}（{doc_id}）")
            success += 1
        except Exception as exc:  # noqa: BLE001 - 单条失败不阻断后续导入
            print(f"[{idx}/{len(documents)}] 导入失败：{doc['name']}（{exc}）")
            failed += 1

    print(f"完成：成功 {success}，失败 {failed}")
    if failed:
        sys.exit(1)


if __name__ == "__main__":
    main()
