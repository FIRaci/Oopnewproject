function drawChart() {
  const canvas = document.createElement('canvas');
  document.body.appendChild(canvas);
  const ctx = canvas.getContext('2d');
  canvas.width = 300;
  canvas.height = 200;

  const folders = ['Work', 'Personal', 'Important'];
  const counts = [5, 3, 2];
  const total = counts.reduce((a, b) => a + b, 0);
  let startAngle = 0;

  ctx.fillStyle = 'lightgray';
  ctx.fillRect(0, 0, canvas.width, canvas.height);

  for (let i = 0; i < folders.length; i++) {
    const angle = (counts[i] / total) * 2 * Math.PI;
    ctx.beginPath();
    ctx.moveTo(150, 100);
    ctx.arc(150, 100, 80, startAngle, startAngle + angle);
    ctx.fillStyle = `hsl(${i * 120}, 70%, 50%)`;
    ctx.fill();
    startAngle += angle;

    const labelX = 150 + 100 * Math.cos(startAngle - angle / 2);
    const labelY = 100 + 100 * Math.sin(startAngle - angle / 2);
    ctx.fillStyle = 'black';
    ctx.font = '12px Arial';
    ctx.fillText(folders[i], labelX, labelY);
  }
}

drawChart();