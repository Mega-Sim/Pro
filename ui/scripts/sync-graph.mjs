import { copyFile, mkdir } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';

const source = resolve(process.cwd(), '..', 'graph_maker', 'graph.json');
const target = resolve(process.cwd(), 'public', 'graph.json');

try {
  await mkdir(dirname(target), { recursive: true });
  await copyFile(source, target);
  console.log(`synced graph file: ${source} -> ${target}`);
} catch (error) {
  console.warn('graph_maker/graph.json 파일을 복사하지 못했습니다. UI에서 파일 업로드로 불러오세요.');
  console.warn(error.message);
}
