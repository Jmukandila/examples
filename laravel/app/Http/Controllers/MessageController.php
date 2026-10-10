<?php

namespace App\Http\Controllers;

use App\Jobs\PublishMessage;
use App\Models\Message;
use Illuminate\Http\RedirectResponse;
use Illuminate\Http\Request;
use Illuminate\View\View;

class MessageController extends Controller
{
    public function index(): View
    {
        return view('messages', [
            'messages' => Message::published()->latest('published_at')->limit(50)->get(),
            'pending' => Message::pending()->count(),
            'hostname' => gethostname(),
        ]);
    }

    public function store(Request $request): RedirectResponse
    {
        $validated = $request->validate([
            'author' => ['required', 'string', 'max:60'],
            'body' => ['required', 'string', 'max:500'],
        ]);

        PublishMessage::dispatch(Message::create($validated));

        return redirect()->route('messages.index')->with('status', 'Message queued.');
    }
}
